import { cache } from "react";
import { redirect } from "next/navigation";
import { createClient } from "@/lib/supabase/server";
import { can, isStaffRole, type Capability } from "@/lib/roles";
import type { Staff } from "@/lib/types";

export function isMissingRelation(error: { code?: string; message?: string } | null) {
  if (!error) return false;
  const message = error.message ?? "";
  return (
    error.code === "PGRST205" ||
    error.code === "42P01" ||
    /schema cache|does not exist|could not find the table/i.test(message)
  );
}

/**
 * The signed-in desk user and their staff row. One staff read per request (React cache),
 * shared by the layout, the page, and anything else that asks.
 */
export const requireDesk = cache(async () => {
  const supabase = await createClient();
  const { data } = await supabase.auth.getClaims();
  if (!data?.claims) {
    redirect("/login");
  }

  const email = String(data.claims.email ?? "");
  const userId = String(data.claims.sub ?? "");

  const { data: row, error } = await supabase
    .from("staff")
    .select("user_id, email, role")
    .eq("user_id", userId)
    .maybeSingle();

  const staff = row && isStaffRole(row.role) ? (row as Staff) : null;
  return {
    supabase,
    email,
    userId,
    staff,
    needsSetup: isMissingRelation(error),
  };
});

export type Desk = Awaited<ReturnType<typeof requireDesk>>;
export type StaffDesk = Desk & { staff: Staff };

/** For pages: the staff desk, or a reason it can't be shown. */
export async function requireStaff(capability?: Capability): Promise<
  { ok: true; desk: StaffDesk } | { ok: false; reason: "setup" | "not-staff" | "forbidden"; desk: Desk }
> {
  const desk = await requireDesk();
  if (desk.needsSetup) return { ok: false, reason: "setup", desk };
  if (!desk.staff) return { ok: false, reason: "not-staff", desk };
  if (capability && !can(desk.staff.role, capability)) return { ok: false, reason: "forbidden", desk };
  return { ok: true, desk: desk as StaffDesk };
}
