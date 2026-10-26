/** A family member the desk sees with a check-in: name and relation only. */
export type FamilyMember = { name: string; relation: string };

const RELATIONS: Record<string, string> = {
  mother: "mother",
  father: "father",
  spouse: "spouse",
  son: "son",
  daughter: "daughter",
  grandmother: "grandmother",
  grandfather: "grandfather",
  sibling: "sibling",
  other: "family",
};

/** The family list desk-check-in attaches to a member, if any. Older servers send none. */
export function familyOf(member: unknown): FamilyMember[] {
  const family = (member as { family?: unknown } | null | undefined)?.family;
  if (!Array.isArray(family)) return [];
  return family
    .filter((row): row is FamilyMember => typeof row?.name === "string" && typeof row?.relation === "string")
    .map((row) => ({ name: row.name, relation: row.relation }));
}

/** "Amma (mother), Papa (father)". */
export function familyLine(family: FamilyMember[]): string {
  return family.map((f) => `${f.name} (${RELATIONS[f.relation] ?? "family"})`).join(", ");
}
