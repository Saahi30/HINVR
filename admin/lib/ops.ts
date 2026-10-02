import type { DeskRequest, DeskRequestRow, Mandir, MemberRow, YoutubeQuota } from "./types";
import { YOUTUBE_DAILY_LIMIT } from "./types";

export function catalogHealth(mandirs: Mandir[]) {
  const live = mandirs.filter((row) => row.live);
  const liveNoUrl = live.filter((row) => !row.live_url?.trim());
  const liveDraft = live.filter((row) => !row.published);
  const missingPhoto = mandirs.filter((row) => !row.photo_url?.trim());
  const drafts = mandirs.filter((row) => !row.published);
  return { live, liveNoUrl, liveDraft, missingPhoto, drafts };
}

export function isPaying(tier: string) {
  return Boolean(tier) && tier !== "None";
}

type ProfileLite = Pick<MemberRow, "id" | "display_name" | "city" | "phone_e164" | "tier">;

export function attachMembers(requests: DeskRequest[], profiles: ProfileLite[]): DeskRequestRow[] {
  const byId = new Map(profiles.map((row) => [row.id, row]));
  return requests.map((row) => {
    const profile = byId.get(row.user_id);
    return {
      ...row,
      display_name: profile?.display_name || "Unnamed",
      city: row.city || profile?.city || "",
      phone_e164: profile?.phone_e164 || "",
      tier: profile?.tier || "None",
    };
  });
}

export function formatWhen(iso: string) {
  const date = new Date(iso);
  if (Number.isNaN(date.getTime())) return iso;
  return new Intl.DateTimeFormat("en-IN", {
    day: "numeric",
    month: "short",
    hour: "2-digit",
    minute: "2-digit",
  }).format(date);
}

export function youtubeQuotaDay() {
  return new Intl.DateTimeFormat("en-CA", {
    timeZone: "America/Los_Angeles",
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
  }).format(new Date());
}

export function parseYoutubeQuota(raw: unknown): YoutubeQuota {
  const value = raw && typeof raw === "object" ? (raw as Record<string, unknown>) : {};
  const today = youtubeQuotaDay();
  const day = String(value.day ?? "");
  const sameDay = day === today;
  const lastRaw = value.last && typeof value.last === "object" ? (value.last as Record<string, unknown>) : null;
  return {
    day: sameDay ? day : today,
    used: sameDay ? Number(value.used) || 0 : 0,
    limit: Number(value.limit) || YOUTUBE_DAILY_LIMIT,
    runs: sameDay ? Number(value.runs) || 0 : 0,
    last:
      sameDay && lastRaw
        ? {
            at: String(lastRaw.at ?? ""),
            units: Number(lastRaw.units) || 0,
            videos: Number(lastRaw.videos) || 0,
            playlists: Number(lastRaw.playlists) || 0,
            channels: Number(lastRaw.channels) || 0,
            searches: Number(lastRaw.searches) || 0,
            live: Number(lastRaw.live) || 0,
            replaced: Number(lastRaw.replaced) || 0,
          }
        : undefined,
  };
}
