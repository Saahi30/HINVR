export type YoutubeRef = { video_id: string; channel_id: string };

const VIDEO_ID = /^[A-Za-z0-9_-]{11}$/;
const CHANNEL_ID = /^UC[A-Za-z0-9_-]{22}$/;
const HANDLE = /^@[A-Za-z0-9._-]{3,30}$/;

export function parseYoutubeRef(input: string): YoutubeRef | null {
  const raw = input.trim();
  if (!raw) return null;
  if (VIDEO_ID.test(raw)) return { video_id: raw, channel_id: "" };
  if (CHANNEL_ID.test(raw) || HANDLE.test(raw)) return { video_id: "", channel_id: raw };

  let url: URL;
  try {
    url = new URL(raw.includes("://") ? raw : `https://${raw}`);
  } catch {
    return null;
  }
  const host = url.hostname.replace(/^www\.|^m\./, "");
  const parts = url.pathname.split("/").filter(Boolean);

  if (host === "youtu.be" && VIDEO_ID.test(parts[0] ?? "")) return { video_id: parts[0], channel_id: "" };
  if (!host.endsWith("youtube.com") && !host.endsWith("youtube-nocookie.com")) return null;

  const v = url.searchParams.get("v") ?? "";
  if (VIDEO_ID.test(v)) return { video_id: v, channel_id: "" };

  const channel = url.searchParams.get("channel") ?? "";
  if (CHANNEL_ID.test(channel)) return { video_id: "", channel_id: channel };

  const [first, second] = parts;
  if (["embed", "live", "shorts"].includes(first ?? "") && second !== "live_stream" && VIDEO_ID.test(second ?? "")) {
    return { video_id: second, channel_id: "" };
  }
  if (first === "channel" && CHANNEL_ID.test(second ?? "")) return { video_id: "", channel_id: second };
  if (first && HANDLE.test(first)) return { video_id: "", channel_id: first };
  return null;
}

export function watchUrl(source: YoutubeRef) {
  if (source.video_id) return `https://www.youtube.com/watch?v=${source.video_id}`;
  if (source.channel_id.startsWith("@")) return `https://www.youtube.com/${source.channel_id}/live`;
  return `https://www.youtube.com/channel/${source.channel_id}/live`;
}
