/** Signed desk credential. HNV1.<kid>.<payload>.<sig> — ECDSA P-256, raw signature.
 * Keep this in step with supabase/functions/_shared/pass-token.ts.
 */

export const DESK_TTL_SECONDS = 6 * 60 * 60;
export const WALLET_TTL_SECONDS = 24 * 60 * 60;
const CLOCK_SKEW_SECONDS = 90;
const MAX_TOKEN_LENGTH = 2048;

export type PassPurpose = "desk" | "wallet";

export type PassClaims = {
  j: string;
  u: string;
  m: string;
  t: string;
  v: number;
  e: number;
  g: number;
  p: PassPurpose;
};

export type VerifiedPass = PassClaims & { kid: string };

export class PassTokenError extends Error {
  constructor(message: string) {
    super(message);
    this.name = "PassTokenError";
  }
}

export function membershipEndUnix(value: string): number | null {
  const text = value.trim();
  if (!text) return null;
  const iso = /^(\d{4})-(\d{2})-(\d{2})/.exec(text);
  if (iso) {
    return Math.floor(Date.UTC(Number(iso[1]), Number(iso[2]) - 1, Number(iso[3]), 23, 59, 59) / 1000);
  }
  const parsed = Date.parse(text);
  if (Number.isNaN(parsed)) return null;
  const date = new Date(parsed);
  return Math.floor(Date.UTC(date.getFullYear(), date.getMonth(), date.getDate(), 23, 59, 59) / 1000);
}

export function bytesToB64url(bytes: Uint8Array): string {
  let binary = "";
  for (const byte of bytes) binary += String.fromCharCode(byte);
  return btoa(binary).replaceAll("+", "-").replaceAll("/", "_").replace(/=+$/g, "");
}

export function b64urlToBytes(value: string): Uint8Array<ArrayBuffer> {
  const padded = value.replaceAll("-", "+").replaceAll("_", "/") + "===".slice((value.length + 3) % 4);
  const binary = atob(padded);
  const bytes = new Uint8Array(binary.length);
  for (let index = 0; index < binary.length; index += 1) bytes[index] = binary.charCodeAt(index);
  return bytes;
}

function payloadJson(claims: PassClaims): string {
  return JSON.stringify({
    j: claims.j,
    u: claims.u,
    m: claims.m,
    t: claims.t,
    v: claims.v,
    e: claims.e,
    g: claims.g,
    p: claims.p,
  });
}

function signingKey(jwk: JsonWebKey, usage: "sign" | "verify"): JsonWebKey {
  const copy: JsonWebKey = {
    kty: jwk.kty,
    crv: jwk.crv,
    x: jwk.x,
    y: jwk.y,
  };
  if (usage === "sign" && jwk.d) copy.d = jwk.d;
  return copy;
}

async function ecdsaKey(jwk: JsonWebKey, usage: "sign" | "verify"): Promise<CryptoKey> {
  return crypto.subtle.importKey(
    "jwk",
    signingKey(jwk, usage),
    { name: "ECDSA", namedCurve: "P-256" },
    false,
    [usage],
  );
}

export async function signPassToken(privateJwk: JsonWebKey, kid: string, claims: PassClaims): Promise<string> {
  if (!kid || kid.includes(".")) throw new PassTokenError("That code is not a signed pass.");
  const payload = bytesToB64url(new TextEncoder().encode(payloadJson(claims)));
  const key = await ecdsaKey(privateJwk, "sign");
  const signature = new Uint8Array(
    await crypto.subtle.sign({ name: "ECDSA", hash: "SHA-256" }, key, new TextEncoder().encode(`${kid}.${payload}`)),
  );
  return `HNV1.${kid}.${payload}.${bytesToB64url(signature)}`;
}

export async function verifyPassToken(
  token: string,
  keys: ReadonlyMap<string, JsonWebKey>,
  nowSeconds = Math.floor(Date.now() / 1000),
): Promise<VerifiedPass> {
  const text = token.trim();
  if (!text.startsWith("HNV1.") || text.length > MAX_TOKEN_LENGTH) {
    throw new PassTokenError("That code is not a signed pass.");
  }
  const parts = text.split(".");
  if (parts.length !== 4 || parts[0] !== "HNV1" || !parts[1] || !parts[2] || !parts[3]) {
    throw new PassTokenError("That code is not a signed pass.");
  }
  const kid = parts[1];
  const jwk = keys.get(kid);
  if (!jwk) throw new PassTokenError("This desk does not have the key for that code yet.");
  let signature: Uint8Array<ArrayBuffer>;
  let payloadBytes: Uint8Array<ArrayBuffer>;
  try {
    payloadBytes = b64urlToBytes(parts[2]);
    signature = b64urlToBytes(parts[3]);
  } catch {
    throw new PassTokenError("That code is not a signed pass.");
  }
  const key = await ecdsaKey(jwk, "verify");
  const ok = await crypto.subtle.verify(
    { name: "ECDSA", hash: "SHA-256" },
    key,
    signature,
    new TextEncoder().encode(`${kid}.${parts[2]}`),
  );
  if (!ok) throw new PassTokenError("That signature does not match.");
  let claims: PassClaims;
  try {
    claims = JSON.parse(new TextDecoder().decode(payloadBytes)) as PassClaims;
  } catch {
    throw new PassTokenError("That code is not a signed pass.");
  }
  if (!claims.j || !claims.u || !claims.m || !claims.t || (claims.p !== "desk" && claims.p !== "wallet")) {
    throw new PassTokenError("That code is not a signed pass.");
  }
  if (!Number.isFinite(claims.e) || nowSeconds > claims.e + CLOCK_SKEW_SECONDS) {
    throw new PassTokenError("This code has expired. Ask them to reopen the pass.");
  }
  if (!Number.isFinite(claims.v) || nowSeconds > claims.v + CLOCK_SKEW_SECONDS) {
    throw new PassTokenError("This membership has ended.");
  }
  if (!Number.isInteger(claims.g) || claims.g < 0) {
    throw new PassTokenError("That code is not a signed pass.");
  }
  return { ...claims, kid };
}
