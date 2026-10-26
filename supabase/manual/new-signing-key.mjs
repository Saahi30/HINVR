// Generates a new P-256 pass signing key for rotation. Node 20+.
//   node supabase/manual/new-signing-key.mjs 2026-11
// Prints the private JWK to merge into PASS_SIGNING_KEYS. Keep it out of git and chat.
const kid = process.argv[2] ?? new Date().toISOString().slice(0, 7);
if (!/^[A-Za-z0-9_-]{1,64}$/.test(kid)) {
  console.error("kid must be 1-64 letters, digits, _ or -");
  process.exit(1);
}
const pair = await crypto.subtle.generateKey({ name: "ECDSA", namedCurve: "P-256" }, true, ["sign", "verify"]);
const jwk = await crypto.subtle.exportKey("jwk", pair.privateKey);
const privateJwk = { kty: jwk.kty, crv: jwk.crv, x: jwk.x, y: jwk.y, d: jwk.d };
console.log(`kid: ${kid}`);
console.log(`entry for PASS_SIGNING_KEYS: ${JSON.stringify({ [kid]: privateJwk })}`);
