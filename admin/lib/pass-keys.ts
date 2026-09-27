import type { SupabaseClient } from "@supabase/supabase-js";

const STORAGE_KEY = "hinvr.pass.publicKeys";

type StoredKey = { kid: string; public_jwk: JsonWebKey };

function readCache(): Map<string, JsonWebKey> {
  if (typeof window === "undefined") return new Map();
  try {
    const parsed = JSON.parse(window.localStorage.getItem(STORAGE_KEY) || "[]") as StoredKey[];
    return new Map(parsed.filter((row) => row.kid && row.public_jwk).map((row) => [row.kid, row.public_jwk]));
  } catch {
    return new Map();
  }
}

function writeCache(keys: Map<string, JsonWebKey>) {
  if (typeof window === "undefined") return;
  const rows = [...keys.entries()].map(([kid, public_jwk]) => ({ kid, public_jwk }));
  window.localStorage.setItem(STORAGE_KEY, JSON.stringify(rows));
}

export async function loadPassPublicKeys(
  supabase: SupabaseClient,
): Promise<{ keys: Map<string, JsonWebKey>; missing: boolean }> {
  const cached = readCache();
  const { data, error } = await supabase.from("pass_signing_keys").select("kid, public_jwk, active").eq("active", true);
  const missing = Boolean(
    error && (error.code === "PGRST205" || error.code === "42P01" || /schema cache|does not exist|could not find/i.test(error.message)),
  );
  if (error || !data) return { keys: cached, missing };
  const next = new Map<string, JsonWebKey>();
  for (const row of data) {
    if (row.kid && row.public_jwk) next.set(row.kid, row.public_jwk as JsonWebKey);
  }
  if (next.size > 0) writeCache(next);
  return { keys: next.size > 0 ? next : cached, missing: false };
}
