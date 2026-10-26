// Same rules as private.check_catalog_urls() in the database.

export const MEDIA_HOSTS = [
  "youtube.com",
  "www.youtube.com",
  "m.youtube.com",
  "youtu.be",
  "youtube-nocookie.com",
  "www.youtube-nocookie.com",
] as const;

function httpsHost(value: string): string | null {
  try {
    const url = new URL(value.trim());
    if (url.protocol !== "https:" || url.username || url.password) return null;
    return url.hostname.toLowerCase();
  } catch {
    return null;
  }
}

/** "" when fine (empty is fine), otherwise what is wrong. */
export function checkHttpsUrl(value: string, label: string): string {
  if (!value.trim()) return "";
  return httpsHost(value) ? "" : `${label} must be a full https:// link.`;
}

export function checkMediaUrl(value: string, label: string): string {
  if (!value.trim()) return "";
  const host = httpsHost(value);
  if (!host) return `${label} must be a full https:// link.`;
  return (MEDIA_HOSTS as readonly string[]).includes(host) ? "" : `${label} must be a YouTube link.`;
}

const PUBLIC_MEDIA = "/storage/v1/object/public/media/";

/** The storage path for a URL in our public media bucket, or null. */
export function mediaPathFromUrl(value: string): string | null {
  const base = process.env.NEXT_PUBLIC_SUPABASE_URL;
  if (!value || !base) return null;
  const prefix = `${base.replace(/\/+$/, "")}${PUBLIC_MEDIA}`;
  return value.startsWith(prefix) ? decodeURIComponent(value.slice(prefix.length)) : null;
}
