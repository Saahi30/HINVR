/**
 * Signed desk credential. HNV1.<kid>.<payload>.<sig> — ECDSA P-256 (SHA-256), raw r||s signature.
 *
 * Canonical source. The admin desk imports this file directly (admin/lib/pass-token.ts
 * re-exports it), so keep it pure Web Crypto: no Deno or Node APIs, no imports.
 */

export const DESK_TTL_SECONDS = 6 * 60 * 60;
export const WALLET_TTL_SECONDS = 24 * 60 * 60;
export const CLOCK_SKEW_SECONDS = 90;
export const MAX_TOKEN_LENGTH = 2048;
/** Memberships end at 23:59:59 India time on their last day. */
const IST_OFFSET_SECONDS = 5 * 60 * 60 + 30 * 60;

export type PassPurpose = "desk" | "wallet";

export type PassClaims = {
  /** credential id */
  j: string;
  /** user id */
  u: string;
  /** member id */
  m: string;
  /** tier */
  t: string;
  /** membership end, unix seconds */
  v: number;
  /** token expiry, unix seconds */
  e: number;
  /** pass generation */
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

/** Unix seconds for 23:59:59 IST on a YYYY-MM-DD date. Null for anything else. */
export function membershipEndUnix(value: string | null | undefined): number | null {
  const text = (value ?? "").trim();
  const iso = /^(\d{4})-(\d{2})-(\d{2})$/.exec(text.slice(0, 10));
  if (!iso) return null;
  const year = Number(iso[1]);
  const month = Number(iso[2]);
  const day = Number(iso[3]);
  const utcMidnight = Date.UTC(year, month - 1, day);
  const check = new Date(utcMidnight);
  if (check.getUTCFullYear() !== year || check.getUTCMonth() !== month - 1 || check.getUTCDate() !== day) return null;
  return Math.floor(utcMidnight / 1000) + 24 * 60 * 60 - 1 - IST_OFFSET_SECONDS;
}

export function bytesToB64url(bytes: Uint8Array): string {
  let binary = "";
  for (const byte of bytes) binary += String.fromCharCode(byte);
  return btoa(binary).replaceAll("+", "-").replaceAll("/", "_").replace(/=+$/g, "");
}

export function b64urlToBytes(value: string): Uint8Array<ArrayBuffer> {
  if (!/^[A-Za-z0-9_-]*$/.test(value)) throw new PassTokenError("That code is not a signed pass.");
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

/** Strips a JWK down to the EC fields, keeping `d` only for signing. */
export function ecJwk(jwk: JsonWebKey, usage: "sign" | "verify"): JsonWebKey {
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
    ecJwk(jwk, usage),
    { name: "ECDSA", namedCurve: "P-256" },
    false,
    [usage],
  );
}

export function isValidKid(kid: string): boolean {
  return /^[A-Za-z0-9_-]{1,64}$/.test(kid);
}

export async function signPassToken(privateJwk: JsonWebKey, kid: string, claims: PassClaims): Promise<string> {
  if (!isValidKid(kid)) throw new PassTokenError("Bad signing key id.");
  if (!privateJwk.d) throw new PassTokenError("That key cannot sign.");
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
  if (signature.byteLength !== 64) throw new PassTokenError("That signature does not match.");
  let key: CryptoKey;
  try {
    key = await ecdsaKey(jwk, "verify");
  } catch {
    throw new PassTokenError("This desk has a broken key for that code.");
  }
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
  if (
    !claims ||
    typeof claims.j !== "string" ||
    typeof claims.u !== "string" ||
    typeof claims.m !== "string" ||
    typeof claims.t !== "string" ||
    !claims.j ||
    !claims.u ||
    !claims.m ||
    !claims.t ||
    (claims.p !== "desk" && claims.p !== "wallet")
  ) {
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
