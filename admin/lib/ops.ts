import type { DeskRequest, DeskRequestRow, Mandir, MemberRow } from "./types";

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
