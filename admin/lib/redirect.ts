const BASE = "https://desk.invalid";

/**
 * A same-origin path to send the user to after sign-in, or the fallback.
 * Only "/something" is accepted: no scheme, no "//host", no backslashes (browsers read
 * "/\evil.com" as "//evil.com"), no control characters.
 */
export function safeNextPath(value: string | null | undefined, fallback = "/"): string {
  if (typeof value !== "string" || value === "") return fallback;
  if (value.length > 512) return fallback;
  if (!value.startsWith("/") || value.startsWith("//")) return fallback;
  if (/[\\\u0000-\u001f\u007f]/.test(value)) return fallback;
  let parsed: URL;
  try {
    parsed = new URL(value, BASE);
  } catch {
    return fallback;
  }
  if (parsed.origin !== BASE) return fallback;
  if (parsed.pathname.startsWith("/auth/") || parsed.pathname === "/login") return fallback;
  return `${parsed.pathname}${parsed.search}${parsed.hash}`;
}
