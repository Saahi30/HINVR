import assert from "node:assert/strict";
import { test } from "node:test";
import { familyLine, familyOf } from "./family.ts";

test("reads the family desk-check-in attaches", () => {
  assert.deepEqual(familyOf({ family: [{ name: "Amma", relation: "mother" }, { bad: 1 }] }), [{ name: "Amma", relation: "mother" }]);
});

test("is empty for an older server", () => {
  assert.deepEqual(familyOf({ display_name: "A" }), []);
  assert.deepEqual(familyOf(undefined), []);
});

test("names each with their relation", () => {
  assert.equal(familyLine([{ name: "Amma", relation: "mother" }, { name: "Ravi", relation: "other" }]), "Amma (mother), Ravi (family)");
});
