import assert from "node:assert/strict";
import { test } from "node:test";
import { formatAartiTimes, parseAartiTimes } from "./aarti-times.ts";

test("pads, sorts and drops repeats", () => {
  assert.deepEqual(parseAartiTimes("19:30, 6:00 6:00").times, ["06:00", "19:30"]);
});

test("blank means no times", () => {
  assert.deepEqual(parseAartiTimes("  ,  "), { times: null, error: "" });
});

test("refuses what the database would refuse", () => {
  for (const value of ["7pm", "24:00", "6:5", "06:60", "6.30"]) {
    assert.ok(parseAartiTimes(value).error, value);
  }
  const many = Array.from({ length: 13 }, (_, i) => `${String(i + 5).padStart(2, "0")}:00`).join(",");
  assert.ok(parseAartiTimes(many).error);
});

test("round-trips through the form", () => {
  assert.equal(formatAartiTimes(["04:30", "18:45"]), "04:30, 18:45");
  assert.equal(formatAartiTimes(null), "");
});
