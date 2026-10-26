import type { SupabaseClient } from "jsr:@supabase/supabase-js@2";
import { ecJwk, isValidKid } from "./pass-token.ts";

/**
 * Pass signing keys come from function secrets, never from the database:
 *   PASS_SIGNING_KEYS       {"<kid>": <private P-256 JWK>, ...}  (old kids stay while their tokens live)
 *   PASS_SIGNING_ACTIVE_KID the kid that signs new tokens
 * The database only stores the public halves (public.pass_signing_keys) for desks.
 */

type KeyRing = { activeKid: string; privateKeys: Map<string, JsonWebKey> };

let ring: KeyRing | null = null;
const registered = new Set<string>();

export class SigningConfigError extends Error {}

function loadRing(): KeyRing {
  if (ring) return ring;
  const raw = Deno.env.get("PASS_SIGNING_KEYS")?.trim() ?? "";
  const activeKid = Deno.env.get("PASS_SIGNING_ACTIVE_KID")?.trim() ?? "";
  if (!raw || !activeKid) throw new SigningConfigError("Pass signing is not set up yet.");
  let parsed: Record<string, JsonWebKey>;
  try {
    parsed = JSON.parse(raw) as Record<string, JsonWebKey>;
  } catch {
    throw new SigningConfigError("PASS_SIGNING_KEYS is not valid JSON.");
  }
  const privateKeys = new Map<string, JsonWebKey>();
  for (const [kid, jwk] of Object.entries(parsed ?? {})) {
    if (!isValidKid(kid) || !jwk || jwk.kty !== "EC" || jwk.crv !== "P-256" || !jwk.x || !jwk.y || !jwk.d) {
      throw new SigningConfigError(`PASS_SIGNING_KEYS has a bad entry for ${kid}.`);
    }
    privateKeys.set(kid, jwk);
  }
  if (!privateKeys.has(activeKid)) throw new SigningConfigError("PASS_SIGNING_ACTIVE_KID is not in PASS_SIGNING_KEYS.");
  ring = { activeKid, privateKeys };
  return ring;
}

/** The key that signs new tokens. Registers its public half once per isolate. */
export async function activeSigningKey(admin: SupabaseClient): Promise<{ kid: string; privateJwk: JsonWebKey }> {
  const { activeKid, privateKeys } = loadRing();
  const privateJwk = privateKeys.get(activeKid)!;
  if (!registered.has(activeKid)) {
    const { error } = await admin.rpc("register_pass_public_key", {
      p_kid: activeKid,
      p_public_jwk: ecJwk(privateJwk, "verify"),
    });
    if (error) throw new SigningConfigError(`Couldn't register the pass key: ${error.message}`);
    registered.add(activeKid);
  }
  return { kid: activeKid, privateJwk };
}

let verifyCache: { at: number; keys: Map<string, JsonWebKey> } | null = null;

/** Public keys a desk should accept: every key in the secret plus every unexpired key in the database. */
export async function verifyKeys(admin: SupabaseClient): Promise<Map<string, JsonWebKey>> {
  if (verifyCache && Date.now() - verifyCache.at < 5 * 60 * 1000) return verifyCache.keys;
  const keys = new Map<string, JsonWebKey>();
  const nowIso = new Date().toISOString();
  const { data } = await admin
    .from("pass_signing_keys")
    .select("kid, public_jwk, verify_until")
    .or(`verify_until.is.null,verify_until.gt.${nowIso}`);
  for (const row of data ?? []) {
    if (row.kid && row.public_jwk) keys.set(String(row.kid), ecJwk(row.public_jwk as JsonWebKey, "verify"));
  }
  try {
    for (const [kid, jwk] of loadRing().privateKeys) keys.set(kid, ecJwk(jwk, "verify"));
  } catch {
    // Verification can still use the database keys.
  }
  verifyCache = { at: Date.now(), keys };
  return keys;
}
