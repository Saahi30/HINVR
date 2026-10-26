import assert from "node:assert/strict";
import { test } from "node:test";
import { safeNextPath } from "./redirect.ts";

test("keeps same-origin paths", () => {
  assert.equal(safeNextPath("/members"), "/members");
  assert.equal(safeNextPath("/members?q=asha&page=2"), "/members?q=asha&page=2");
  assert.equal(safeNextPath("/mandirs/kashi#live"), "/mandirs/kashi#live");
});

test("refuses anything that can leave the site", () => {
  for (const value of [
    "//evil.com",
    "//evil.com/members",
    "/\\evil.com",
    "\\\\evil.com",
    "https://evil.com",
    "javascript:alert(1)",
    "evil.com",
    " /members",
    "/members\nSet-Cookie: x",
    "/%0d%0a",
    "",
    null,
    undefined,
  ]) {
    const result = safeNextPath(value as string | null | undefined);
    assert.ok(result === "/" || result.startsWith("/%"), `${JSON.stringify(value)} -> ${result}`);
    assert.ok(!result.startsWith("//"));
  }
});

test("does not loop back into auth pages", () => {
  assert.equal(safeNextPath("/login"), "/");
  assert.equal(safeNextPath("/auth/callback?code=x"), "/");
});

test("uses the fallback", () => {
  assert.equal(safeNextPath("//evil.com", "/scan"), "/scan");
});
