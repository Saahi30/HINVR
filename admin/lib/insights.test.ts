import assert from "node:assert/strict";
import { test } from "node:test";
import { fillDays, istDate, percent, periodFrom, ranked } from "./insights.ts";

test("only the offered periods, else 30", () => {
  assert.equal(periodFrom("7"), 7);
  assert.equal(periodFrom(["90"]), 90);
  assert.equal(periodFrom("14"), 30);
  assert.equal(periodFrom(undefined), 30);
});

test("India dates cross midnight at 18:30 UTC", () => {
  assert.equal(istDate("2026-10-26T18:29:00Z"), "2026-10-26");
  assert.equal(istDate("2026-10-26T18:30:00+00:00"), "2026-10-27");
});

test("days with nothing offered still get a bar", () => {
  const days = fillDays([{ day: "2026-10-27", total: 4, kinds: { diya: 4 } }], "2026-10-24T18:30:00Z", 3);
  assert.deepEqual(days.map((d) => [d.day, d.total]), [["2026-10-25", 0], ["2026-10-26", 0], ["2026-10-27", 4]]);
});

test("percent and ranking", () => {
  assert.equal(percent(1, 3), 33);
  assert.equal(percent(5, 0), 0);
  assert.deepEqual(ranked({ diya: 2, aarti: 5, ghanta: 2 }), [["aarti", 5], ["diya", 2], ["ghanta", 2]]);
});
