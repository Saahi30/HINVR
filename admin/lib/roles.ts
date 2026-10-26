export const STAFF_ROLES = ["owner", "admin", "finance", "desk", "editor"] as const;
export type StaffRole = (typeof STAFF_ROLES)[number];

export const ROLE_LABELS: Record<StaffRole, string> = {
  owner: "Owner",
  admin: "Admin",
  finance: "Finance",
  desk: "Desk",
  editor: "Editor",
};

export const ROLE_HINTS: Record<StaffRole, string> = {
  owner: "Everything, including the team.",
  admin: "Everything except the team.",
  finance: "Memberships, invoices, audit.",
  desk: "Scanner, desk requests, card list.",
  editor: "Catalog and home screen.",
};

/** Mirrors the role checks in the database policies. The database is the real gate. */
const CAN = {
  catalog: ["owner", "admin", "editor"],
  deleteMandir: ["owner", "admin"],
  members: ["owner", "admin", "finance", "desk"],
  membership: ["owner", "admin", "finance"],
  requests: ["owner", "admin", "finance", "desk"],
  scan: ["owner", "admin", "desk"],
  live: ["owner", "admin", "editor"],
  notify: ["owner", "admin"],
  audit: ["owner", "admin", "finance"],
  team: ["owner"],
  setupDetails: ["owner"],
} as const satisfies Record<string, readonly StaffRole[]>;

export type Capability = keyof typeof CAN;

export function isStaffRole(value: unknown): value is StaffRole {
  return typeof value === "string" && (STAFF_ROLES as readonly string[]).includes(value);
}

export function can(role: StaffRole | null | undefined, capability: Capability): boolean {
  return role != null && (CAN[capability] as readonly StaffRole[]).includes(role);
}
