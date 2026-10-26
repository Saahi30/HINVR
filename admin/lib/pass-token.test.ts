// Tests the one pass-token module the edge functions and the desk share. Run: npm test
import assert from "node:assert/strict";
import { test } from "node:test";
import { classifyScan } from "./pass-code.ts";
import {
  CLOCK_SKEW_SECONDS,
  MAX_TOKEN_LENGTH,
  membershipEndUnix,
  PassTokenError,
  signPassToken,
  verifyPassToken,
  type PassClaims,
} from "../../supabase/functions/_shared/pass-token.ts";

async function keyPair() {
  const pair = await crypto.subtle.generateKey({ name: "ECDSA", namedCurve: "P-256" }, true, ["sign", "verify"]);
  return {
    privateJwk: await crypto.subtle.exportKey("jwk", pair.privateKey),
    publicJwk: await crypto.subtle.exportKey("jwk", pair.publicKey),
  };
}

const now = 1_800_000_000;
const claims: PassClaims = {
  j: "6f0b9c2e-1a2b-4c3d-8e9f-001122334455",
  u: "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee",
  m: "HNV-2026-0001",
  t: "Gold",
  v: now + 86400 * 30,
  e: now + 6 * 60 * 60,
  g: 2,
  p: "desk",
};

function rejectsWith(pattern: RegExp) {
  return (error: unknown) => error instanceof PassTokenError && pattern.test(error.message);
}

test("sign and verify round trip", async () => {
  const { privateJwk, publicJwk } = await keyPair();
  const token = await signPassToken(privateJwk, "primary", claims);
  assert.ok(token.startsWith("HNV1.primary."));
  assert.equal(token.split(".").length, 4);
  const verified = await verifyPassToken(token, new Map([["primary", publicJwk]]), now);
  assert.equal(verified.m, claims.m);
  assert.equal(verified.g, 2);
  assert.equal(verified.kid, "primary");
});

test("expiry and clock skew", async () => {
  const { privateJwk, publicJwk } = await keyPair();
  const keys = new Map([["primary", publicJwk]]);
  const token = await signPassToken(privateJwk, "primary", { ...claims, e: now });
  await verifyPassToken(token, keys, now + CLOCK_SKEW_SECONDS);
  await assert.rejects(() => verifyPassToken(token, keys, now + CLOCK_SKEW_SECONDS + 1), rejectsWith(/expired/i));
  const ended = await signPassToken(privateJwk, "primary", { ...claims, v: now - CLOCK_SKEW_SECONDS - 1 });
  await assert.rejects(() => verifyPassToken(ended, keys, now), rejectsWith(/ended/i));
});

test("unknown kid and wrong key", async () => {
  const { privateJwk, publicJwk } = await keyPair();
  const other = await keyPair();
  const token = await signPassToken(privateJwk, "primary", claims);
  await assert.rejects(() => verifyPassToken(token, new Map([["2026-11", publicJwk]]), now), rejectsWith(/key/i));
  await assert.rejects(
    () => verifyPassToken(token, new Map([["primary", other.publicJwk]]), now),
    rejectsWith(/signature/i),
  );
});

test("tampered payload or kid is refused", async () => {
  const { privateJwk, publicJwk } = await keyPair();
  const keys = new Map([["primary", publicJwk], ["other", publicJwk]]);
  const token = await signPassToken(privateJwk, "primary", claims);
  const [head, kid, payload, signature] = token.split(".");
  const forged = Buffer.from(JSON.stringify({ ...claims, t: "Platinum" })).toString("base64url");
  await assert.rejects(() => verifyPassToken(`${head}.${kid}.${forged}.${signature}`, keys, now), rejectsWith(/signature/i));
  await assert.rejects(() => verifyPassToken(`${head}.other.${payload}.${signature}`, keys, now), rejectsWith(/signature/i));
  await assert.rejects(() => verifyPassToken(`${head}.${kid}.${payload}.${signature}x`, keys, now), PassTokenError);
});

test("length cap and shape", async () => {
  const { publicJwk } = await keyPair();
  const keys = new Map([["primary", publicJwk]]);
  await assert.rejects(() => verifyPassToken(`HNV1.primary.${"a".repeat(MAX_TOKEN_LENGTH)}.b`, keys, now), rejectsWith(/not a signed pass/));
  await assert.rejects(() => verifyPassToken("HNV1.primary.only-three", keys, now), PassTokenError);
  await assert.rejects(() => verifyPassToken("HNV|HNV-2026-0001|Gold", keys, now), PassTokenError);
});

test("signing refuses a bad kid or a public key", async () => {
  const { privateJwk, publicJwk } = await keyPair();
  await assert.rejects(() => signPassToken(privateJwk, "has.dot", claims), PassTokenError);
  await assert.rejects(() => signPassToken(publicJwk, "primary", claims), PassTokenError);
});

test("membership ends 23:59:59 IST", () => {
  assert.equal(membershipEndUnix("2027-09-28"), Date.UTC(2027, 8, 28, 18, 29, 59) / 1000);
  assert.equal(membershipEndUnix("2027-02-31"), null);
  assert.equal(membershipEndUnix("28 Sep 2027"), null);
  assert.equal(membershipEndUnix(""), null);
  assert.equal(membershipEndUnix(null), null);
});

test("scan classification", async () => {
  const { privateJwk } = await keyPair();
  const token = await signPassToken(privateJwk, "primary", claims);
  assert.equal(classifyScan(token, true).kind, "signed");
  assert.equal(classifyScan("HNV|HNV-2026-0001|Gold|28 Sep 2027", true).kind, "rejected");
  assert.equal(classifyScan("HNV-2026-0001", true).kind, "rejected");
  assert.equal(classifyScan("HNV-2026-0001").kind, "manual");
});
