import assert from "node:assert/strict";
import { classifyScan } from "./pass-code.ts";
import { membershipEndUnix, signPassToken, verifyPassToken, PassTokenError } from "../../supabase/functions/_shared/pass-token.ts";

const pair = await crypto.subtle.generateKey({ name: "ECDSA", namedCurve: "P-256" }, true, ["sign", "verify"]);
const privateJwk = await crypto.subtle.exportKey("jwk", pair.privateKey);
const publicJwk = await crypto.subtle.exportKey("jwk", pair.publicKey);
const keys = new Map([["primary", publicJwk]]);
const now = Math.floor(Date.now() / 1000);

const claims = {
  j: "6f0b9c2e-1a2b-4c3d-8e9f-001122334455",
  u: "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee",
  m: "HNV-2026-0001",
  t: "Gold",
  v: now + 86400 * 30,
  e: now + 6 * 60 * 60,
  g: 2,
  p: "desk" as const,
};

const token = await signPassToken(privateJwk, "primary", claims);
assert.equal(token.startsWith("HNV1.primary."), true);
assert.equal(token.includes("|"), false);
const verified = await verifyPassToken(token, keys, now);
assert.equal(verified.m, "HNV-2026-0001");
assert.equal(verified.g, 2);
assert.equal(verified.e, claims.e);

const forged = "HNV|HNV-2026-0001|Gold|28 Sep 2027|aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee";
assert.equal(classifyScan(forged).kind, "rejected");
assert.equal(classifyScan(forged, true).kind, "rejected");
assert.equal(classifyScan("HNV-2026-0001", true).kind, "rejected");
assert.equal(classifyScan("HNV-2026-0001").kind, "manual");
assert.equal(classifyScan(token).kind, "signed");

const parts = token.split(".");
const flipped = parts[2].slice(0, -1) + (parts[2].endsWith("A") ? "B" : "A");
await assert.rejects(() => verifyPassToken(`${parts[0]}.${parts[1]}.${flipped}.${parts[3]}`, keys, now), PassTokenError);

const expired = await signPassToken(privateJwk, "primary", { ...claims, e: now - 120 });
await assert.rejects(
  () => verifyPassToken(expired, keys, now),
  (error) => error instanceof PassTokenError && /expired/i.test(error.message),
);

const ended = await signPassToken(privateJwk, "primary", { ...claims, v: now - 86400 });
await assert.rejects(
  () => verifyPassToken(ended, keys, now),
  (error) => error instanceof PassTokenError && /ended/i.test(error.message),
);

const other = await crypto.subtle.generateKey({ name: "ECDSA", namedCurve: "P-256" }, true, ["sign", "verify"]);
const otherPublic = await crypto.subtle.exportKey("jwk", other.publicKey);
await assert.rejects(
  () => verifyPassToken(token, new Map([["primary", otherPublic]]), now),
  (error) => error instanceof PassTokenError && /signature/i.test(error.message),
);

const isoEnd = membershipEndUnix("2027-09-28");
assert.equal(isoEnd, Math.floor(Date.UTC(2027, 8, 28, 23, 59, 59) / 1000));
assert.equal(membershipEndUnix("28 Sep 2027") != null, true);
assert.equal(membershipEndUnix(""), null);

console.log("pass credential checks passed");
