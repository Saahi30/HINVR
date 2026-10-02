export const YOUTUBE_DAILY_LIMIT = 10_000;

const API = "https://www.googleapis.com/youtube/v3";

const COST: Record<string, number> = {
  videos: 1,
  playlistItems: 1,
  channels: 1,
  search: 100,
};

export type QuotaSnap = {
  units: number;
  videos: number;
  playlists: number;
  channels: number;
  searches: number;
};

const emptySnap = (): QuotaSnap => ({ units: 0, videos: 0, playlists: 0, channels: 0, searches: 0 });

let snap = emptySnap();

export function resetQuotaRun() {
  snap = emptySnap();
}

export function quotaSnap(): QuotaSnap {
  return { ...snap };
}

export function quotaDay(at = new Date()) {
  return new Intl.DateTimeFormat("en-CA", {
    timeZone: "America/Los_Angeles",
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
  }).format(at);
}

function charge(path: string) {
  const units = COST[path] ?? 1;
  snap.units += units;
  if (path === "videos") snap.videos += 1;
  else if (path === "playlistItems") snap.playlists += 1;
  else if (path === "channels") snap.channels += 1;
  else if (path === "search") snap.searches += 1;
}

export type VideoState = {
  id: string;
  title: string;
  channelId: string;
  channelTitle: string;
  live: boolean;
  embeddable: boolean;
  viewers: number | null;
};

type ApiItem = {
  id?: string | { videoId?: string };
  snippet?: { title?: string; channelId?: string; channelTitle?: string; liveBroadcastContent?: string };
  liveStreamingDetails?: { concurrentViewers?: string; actualEndTime?: string };
  status?: { embeddable?: boolean; privacyStatus?: string };
  contentDetails?: { videoId?: string };
};

function videoIdOf(item: ApiItem) {
  if (item.contentDetails?.videoId) return item.contentDetails.videoId;
  if (typeof item.id === "object" && item.id?.videoId) return item.id.videoId;
  return typeof item.id === "string" ? item.id : "";
}

type ApiError = { error?: { message?: string; errors?: { reason?: string }[] } };

async function call(path: string, params: Record<string, string>, key: string): Promise<ApiItem[]> {
  charge(path);
  const query = new URLSearchParams({ ...params, key });
  const res = await fetch(`${API}/${path}?${query}`);
  const body = (await res.json().catch(() => ({}))) as { items?: ApiItem[] } & ApiError;
  if (!res.ok) {
    const reason = body.error?.errors?.[0]?.reason ?? body.error?.message ?? String(res.status);
    throw new Error(`YouTube ${path}: ${reason}`);
  }
  return body.items ?? [];
}

export async function fetchVideos(ids: string[], key: string): Promise<Map<string, VideoState>> {
  const out = new Map<string, VideoState>();
  const unique = [...new Set(ids.filter(Boolean))];
  for (let i = 0; i < unique.length; i += 50) {
    const batch = unique.slice(i, i + 50);
    const items = await call(
      "videos",
      { part: "snippet,liveStreamingDetails,status", id: batch.join(","), maxResults: "50" },
      key,
    );
    for (const item of items) {
      const id = videoIdOf(item);
      if (!id) continue;
      const viewers = Number.parseInt(item.liveStreamingDetails?.concurrentViewers ?? "", 10);
      out.set(id, {
        id,
        title: item.snippet?.title ?? "",
        channelId: item.snippet?.channelId ?? "",
        channelTitle: item.snippet?.channelTitle ?? "",
        live: item.snippet?.liveBroadcastContent === "live" && !item.liveStreamingDetails?.actualEndTime,
        embeddable: item.status?.embeddable === true && item.status?.privacyStatus !== "private",
        viewers: Number.isFinite(viewers) ? viewers : null,
      });
    }
  }
  return out;
}

export async function recentUploads(channelId: string, key: string): Promise<string[]> {
  if (!channelId.startsWith("UC")) return [];
  const items = await call(
    "playlistItems",
    { part: "contentDetails", playlistId: `UU${channelId.slice(2)}`, maxResults: "15" },
    key,
  );
  return items.map(videoIdOf).filter(Boolean);
}

export async function searchLive(query: string, key: string, channelId = ""): Promise<string[]> {
  const q = query.trim();
  if (!q) return [];
  const params: Record<string, string> = {
    part: "snippet",
    type: "video",
    eventType: "live",
    maxResults: "8",
    q,
  };
  if (channelId.startsWith("UC")) params.channelId = channelId;
  const items = await call("search", params, key);
  return items.map(videoIdOf).filter(Boolean);
}

export async function resolveHandle(handle: string, key: string): Promise<string> {
  const items = await call("channels", { part: "id", forHandle: handle }, key);
  const id = items[0]?.id;
  return typeof id === "string" ? id : "";
}

export function embedUrl(videoId: string) {
  return `https://www.youtube.com/embed/${videoId}`;
}
