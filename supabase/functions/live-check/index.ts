import "jsr:@supabase/functions-js/edge-runtime.d.ts";
import { createClient, type SupabaseClient } from "jsr:@supabase/supabase-js@2";
import { json } from "../_shared/http.ts";
import {
  embedUrl,
  fetchVideos,
  quotaDay,
  quotaSnap,
  recentUploads,
  resetQuotaRun,
  resolveHandle,
  searchLive,
  YOUTUBE_DAILY_LIMIT,
  type QuotaSnap,
  type VideoState,
} from "../_shared/youtube.ts";

type Source = {
  id: string;
  mandir_id: string;
  video_id: string;
  channel_id: string;
  channel_name: string;
  official: boolean;
  follow_channel: boolean;
  title_match: string;
  priority: number;
};

type Outcome = { source: Source; video: VideoState | null; error: string };

type MandirRow = { id: string; name: string; city: string; live_searched_at: string | null };

type QuotaRecord = {
  day: string;
  used: number;
  limit: number;
  runs: number;
  last: QuotaSnap & { at: string; live?: number; replaced?: number };
};

async function loadQuota(admin: SupabaseClient) {
  const day = quotaDay();
  const { data } = await admin.from("app_settings").select("value").eq("key", "youtube_quota").maybeSingle();
  const prev = (data?.value ?? null) as QuotaRecord | null;
  const used = prev?.day === day ? Number(prev.used) || 0 : 0;
  const runs = prev?.day === day ? Number(prev.runs) || 0 : 0;
  return { day, used, runs, prev };
}

async function saveQuota(
  admin: SupabaseClient,
  started: { day: string; used: number; runs: number },
  extra: { live?: number; replaced?: number } = {},
) {
  const run = quotaSnap();
  const value: QuotaRecord = {
    day: started.day,
    used: started.used + run.units,
    limit: YOUTUBE_DAILY_LIMIT,
    runs: started.runs + 1,
    last: { at: new Date().toISOString(), ...run, ...extra },
  };
  await admin.from("app_settings").upsert({ key: "youtube_quota", value });
  return value;
}

const SEARCH_COOLDOWN_MS = 3 * 60 * 60 * 1000;
const MAX_SEARCHES = 8;

function sameSecret(a: string, b: string) {
  if (!a || a.length !== b.length) return false;
  let diff = 0;
  for (let i = 0; i < a.length; i++) diff |= a.charCodeAt(i) ^ b.charCodeAt(i);
  return diff === 0;
}

async function allowed(req: Request, url: string, anonKey: string, admin: SupabaseClient) {
  const secret = Deno.env.get("LIVE_CHECK_SECRET") ?? "";
  if (sameSecret(req.headers.get("x-live-check-secret") ?? "", secret)) return true;

  const authHeader = req.headers.get("Authorization") ?? "";
  if (!anonKey || !authHeader.toLowerCase().startsWith("bearer ")) return false;
  const userClient = createClient(url, anonKey, {
    global: { headers: { Authorization: authHeader } },
    auth: { persistSession: false, autoRefreshToken: false },
  });
  const { data } = await userClient.auth.getUser();
  const userId = data.user?.id;
  if (!userId) return false;
  const { data: staff } = await admin.from("staff").select("user_id").eq("user_id", userId).maybeSingle();
  return Boolean(staff);
}

const isPlayable = (video: VideoState | undefined) => Boolean(video?.live && video.embeddable);

function rank(a: Outcome, b: Outcome) {
  return (
    Number(b.source.official) - Number(a.source.official) ||
    a.source.priority - b.source.priority ||
    (b.video?.viewers ?? 0) - (a.video?.viewers ?? 0)
  );
}

function titleFits(title: string, name: string, city: string, extra: string) {
  const blob = title.toLowerCase();
  if (extra.trim() && blob.includes(extra.trim().toLowerCase())) return true;
  const words = `${name} ${city}`
    .toLowerCase()
    .split(/[^a-z0-9]+/)
    .filter((word) => word.length >= 4);
  return words.length === 0 || words.some((word) => blob.includes(word));
}

async function pullVideos(ids: string[], videos: Map<string, VideoState>, key: string) {
  const missing = ids.filter((id) => id && !videos.has(id));
  if (!missing.length) return;
  for (const [id, video] of await fetchVideos(missing, key)) videos.set(id, video);
}

async function findReplacement(
  mandir: MandirRow,
  list: Outcome[],
  videos: Map<string, VideoState>,
  key: string,
  budget: { left: number },
) {
  const target = [...list].sort(rank)[0]?.source;
  if (!target || budget.left <= 0) return null;

  const queries: { q: string; channelId: string }[] = [];
  if (target.channel_id.startsWith("UC")) {
    queries.push({ q: `${mandir.name} live`, channelId: target.channel_id });
  }
  queries.push({ q: `${mandir.name} ${mandir.city} live darshan`.trim(), channelId: "" });

  for (const query of queries) {
    if (budget.left <= 0) break;
    budget.left -= 1;
    let ids: string[] = [];
    try {
      ids = await searchLive(query.q, key, query.channelId);
    } catch {
      continue;
    }
    try {
      await pullVideos(ids, videos, key);
    } catch {
      continue;
    }
    const hit = ids
      .map((id) => videos.get(id))
      .filter((video): video is VideoState => isPlayable(video))
      .filter((video) => titleFits(video.title, mandir.name, mandir.city, target.title_match))
      .sort((a, b) => (b.viewers ?? 0) - (a.viewers ?? 0))[0];
    if (hit) return { target, video: hit };
  }
  return { target, video: null as VideoState | null };
}

Deno.serve(async (req: Request) => {
  if (req.method !== "POST") return json({ error: "Use POST." }, 405);

  const url = Deno.env.get("SUPABASE_URL") ?? "";
  const anonKey = Deno.env.get("SUPABASE_ANON_KEY") ?? "";
  const serviceKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY") ?? Deno.env.get("SUPABASE_SECRET_KEY") ?? "";
  const youtubeKey = Deno.env.get("YOUTUBE_API_KEY") ?? "";
  if (!url || !serviceKey) return json({ error: "Function isn't configured." }, 500);

  let find = false;
  try {
    const payload = await req.json();
    find = payload?.find === true;
  } catch {
    find = false;
  }

  const admin = createClient(url, serviceKey, {
    auth: { persistSession: false, autoRefreshToken: false },
  });
  if (!(await allowed(req, url, anonKey, admin))) return json({ error: "No access." }, 401);
  if (!youtubeKey) return json({ error: "Set the YOUTUBE_API_KEY secret first." }, 500);
  resetQuotaRun();
  const started = await loadQuota(admin);
  if (started.used >= YOUTUBE_DAILY_LIMIT) {
    return json(
      {
        error: "YouTube quota for today is used up. It resets at midnight Pacific.",
        quota: {
          day: started.day,
          used: started.used,
          limit: YOUTUBE_DAILY_LIMIT,
          runs: started.runs,
          last: started.prev?.last,
        },
      },
      429,
    );
  }

  const { data: rows, error: loadError } = await admin
    .from("live_sources")
    .select("id,mandir_id,video_id,channel_id,channel_name,official,follow_channel,title_match,priority")
    .eq("enabled", true);
  if (loadError) return json({ error: "Couldn't load live sources." }, 500);
  const sources = (rows ?? []) as Source[];

  const { data: mandirRows } = await admin
    .from("mandirs")
    .select("id,name,city,live_searched_at")
    .in("id", [...new Set(sources.map((s) => s.mandir_id))]);
  const mandirs = new Map(((mandirRows ?? []) as MandirRow[]).map((row) => [row.id, row]));

  for (const source of sources) {
    if (!source.channel_id.startsWith("@")) continue;
    source.channel_id = await resolveHandle(source.channel_id, youtubeKey).catch(() => "");
  }

  let videos: Map<string, VideoState>;
  try {
    videos = await fetchVideos(sources.map((s) => s.video_id), youtubeKey);
  } catch (error) {
    const quota = await saveQuota(admin, started);
    return json(
      { error: error instanceof Error ? error.message : "YouTube didn't answer.", quota },
      502,
    );
  }

  for (const source of sources) {
    const video = videos.get(source.video_id);
    if (video && !source.channel_id) source.channel_id = video.channelId;
    if (video && !source.channel_name) source.channel_name = video.channelTitle;
  }

  const followers = sources.filter(
    (s) => (s.follow_channel || !s.video_id) && s.channel_id.startsWith("UC") && !isPlayable(videos.get(s.video_id)),
  );
  const uploads = new Map<string, string[]>();
  const channelErrors = new Map<string, string>();
  for (const channelId of new Set(followers.map((s) => s.channel_id))) {
    try {
      uploads.set(channelId, await recentUploads(channelId, youtubeKey));
    } catch (error) {
      channelErrors.set(channelId, error instanceof Error ? error.message : "Channel lookup failed.");
    }
  }
  try {
    await pullVideos([...uploads.values()].flat(), videos, youtubeKey);
  } catch (error) {
    const message = error instanceof Error ? error.message : "YouTube didn't answer.";
    for (const channelId of uploads.keys()) channelErrors.set(channelId, message);
  }

  const outcomes: Outcome[] = sources.map((source) => {
    const current = videos.get(source.video_id);
    if (current && isPlayable(current)) return { source, video: current, error: "" };
    if (followers.includes(source)) {
      const match = source.title_match.trim().toLowerCase();
      const next = (uploads.get(source.channel_id) ?? [])
        .map((id) => videos.get(id))
        .filter((video): video is VideoState => isPlayable(video))
        .filter((video) => !match || video.title.toLowerCase().includes(match))
        .sort((a, b) => (b.viewers ?? 0) - (a.viewers ?? 0))[0];
      if (next) return { source, video: next, error: "" };
      const channelError = channelErrors.get(source.channel_id);
      if (channelError) return { source, video: null, error: channelError };
    }
    if (source.video_id && !current) return { source, video: null, error: "Video not found or removed." };
    if (current && !current.embeddable) return { source, video: null, error: "Owner blocks embedding." };
    return { source, video: null, error: "" };
  });

  const byMandir = new Map<string, Outcome[]>();
  for (const outcome of outcomes) {
    const list = byMandir.get(outcome.source.mandir_id) ?? [];
    list.push(outcome);
    byMandir.set(outcome.source.mandir_id, list);
  }

  const budget = { left: MAX_SEARCHES };
  const searched: string[] = [];
  const replaced: string[] = [];
  const nowMs = Date.now();

  for (const [mandirId, list] of byMandir) {
    if (list.some((o) => o.video)) continue;
    const mandir = mandirs.get(mandirId);
    if (!mandir) continue;
    const last = mandir.live_searched_at ? Date.parse(mandir.live_searched_at) : 0;
    if (!find && last && nowMs - last < SEARCH_COOLDOWN_MS) continue;
    const spent = started.used + quotaSnap().units;
    const headroom = YOUTUBE_DAILY_LIMIT - spent;
    if (headroom < 100) break;
    if (!find && headroom < 500) break;
    if (budget.left <= 0) break;
    const found = await findReplacement(mandir, list, videos, youtubeKey, budget);
    if (!found) continue;
    searched.push(mandirId);
    if (found.video) {
      found.target.video_id = found.video.id;
      found.target.channel_id = found.video.channelId || found.target.channel_id;
      found.target.channel_name = found.video.channelTitle || found.target.channel_name;
      const slot = list.find((o) => o.source.id === found.target.id);
      if (slot) {
        slot.video = found.video;
        slot.error = "";
      }
      replaced.push(mandirId);
    } else {
      const slot = list.find((o) => o.source.id === found.target.id);
      if (slot && !slot.error) slot.error = "No live video found.";
    }
  }

  const now = new Date().toISOString();
  for (const { source, video, error } of outcomes) {
    const seen = video ?? videos.get(source.video_id);
    await admin
      .from("live_sources")
      .update({
        video_id: video?.id ?? source.video_id,
        channel_id: source.channel_id,
        channel_name: source.channel_name || seen?.channelTitle || "",
        last_checked_at: now,
        last_live: Boolean(video),
        last_title: seen?.title ?? "",
        last_viewers: video?.viewers ?? null,
        last_error: error,
      })
      .eq("id", source.id);
  }

  const label = `Feed checked ${new Intl.DateTimeFormat("en-IN", {
    timeZone: "Asia/Kolkata",
    hour: "numeric",
    minute: "2-digit",
  }).format(new Date())} IST`;

  const live: { mandir: string; video: string; channel: string; viewers: number | null }[] = [];
  for (const [mandirId, list] of byMandir) {
    const best = list.filter((o) => o.video).sort(rank)[0];
    await admin
      .from("mandirs")
      .update({
        live: Boolean(best),
        live_url: best?.video ? embedUrl(best.video.id) : "",
        live_checked_at: now,
        updated_label: label,
        ...(searched.includes(mandirId) ? { live_searched_at: now } : {}),
      })
      .eq("id", mandirId);
    if (best?.video) {
      live.push({
        mandir: mandirId,
        video: best.video.id,
        channel: best.source.channel_name || best.video.channelTitle,
        viewers: best.video.viewers,
      });
    }
  }

  const quota = await saveQuota(admin, started, { live: live.length, replaced: replaced.length });
  return json({
    checked: sources.length,
    mandirs: byMandir.size,
    live: live.length,
    rotated: outcomes.filter((o) => o.video && o.video.id !== o.source.video_id).length,
    searched: searched.length,
    replaced: replaced.length,
    quota,
    feeds: live,
  });
});
