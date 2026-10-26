import type { SupabaseClient } from "@supabase/supabase-js";

const STORAGE_KEY = "hinvr.pass.publicKeys.v2";
/** How long cached public keys count as fresh. The scanner refreshes on this interval. */
export const KEY_REFRESH_MS = 30 * 60 * 1000;

type StoredKeys = { fetchedAt: number; keys: Array<{ kid: string; public_jwk: JsonWebKey }> };

function readStored(): StoredKeys | null {
  if (typeof window === "undefined") return null;
  try {
    const parsed = JSON.parse(window.localStorage.getItem(STORAGE_KEY) || "null") as StoredKeys | null;
    if (!parsed || !Array.isArray(parsed.keys)) return null;
    return parsed;
  } catch {
    return null;
  }
}

function writeStored(value: StoredKeys) {
  if (typeof window === "undefined") return;
  try {
    window.localStorage.setItem(STORAGE_KEY, JSON.stringify(value));
  } catch {
    // Private mode or full storage: keys stay in memory for this tab.
  }
}

function toMap(stored: StoredKeys | null) {
  return new Map((stored?.keys ?? []).filter((row) => row.kid && row.public_jwk).map((row) => [row.kid, row.public_jwk]));
}

/** Cached public keys, for verifying a pass while offline. */
export function cachedPassKeys(): { keys: Map<string, JsonWebKey>; fetchedAt: number } {
  const stored = readStored();
  return { keys: toMap(stored), fetchedAt: stored?.fetchedAt ?? 0 };
}

/** Fetches the public keys desks may verify with and caches them. Keeps the cache on failure. */
export async function refreshPassKeys(supabase: SupabaseClient): Promise<{ keys: Map<string, JsonWebKey>; fetchedAt: number }> {
  const { data, error } = await supabase.from("pass_signing_keys").select("kid, public_jwk");
  if (error || !data) return cachedPassKeys();
  const stored: StoredKeys = {
    fetchedAt: Date.now(),
    keys: data
      .filter((row) => row.kid && row.public_jwk)
      .map((row) => ({ kid: String(row.kid), public_jwk: row.public_jwk as JsonWebKey })),
  };
  if (stored.keys.length > 0) writeStored(stored);
  return { keys: toMap(stored.keys.length > 0 ? stored : readStored()), fetchedAt: stored.fetchedAt };
}
