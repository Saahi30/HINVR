import { redirect } from "next/navigation";
import { DeskShell, GateMessage } from "@/components/desk-shell";
import { MembersTable } from "@/components/members-table";
import { PageHeader } from "@/components/ui";
import { requireDesk } from "@/lib/auth";
import type { MemberRow } from "@/lib/types";

export default async function MembersPage() {
  const desk = await requireDesk();
  if (desk.needsSetup) redirect("/setup");
  if (!desk.staff) {
    return <GateMessage title="No access." body="This admin already has an owner." />;
  }

  const full = await desk.supabase
    .from("profiles")
    .select("id, display_name, city, tier, member_id, valid_until, audience, profile_complete, phone_e164, updated_at")
    .order("display_name");

  let rows = (full.data ?? []) as MemberRow[];
  if (full.error) {
    const fallback = await desk.supabase
      .from("profiles")
      .select("id, display_name, city, tier, member_id, valid_until, audience")
      .order("display_name");
    rows = ((fallback.data ?? []) as Omit<MemberRow, "profile_complete" | "phone_e164">[]).map((row) => ({
      ...row,
      profile_complete: false,
      phone_e164: "",
    }));
  }

  return (
    <DeskShell email={desk.email || desk.staff.email} role={desk.staff.role}>
      <PageHeader
        title="Members"
        description="Funnel first: who finished setup, who is still on None, then set tier and pass dates."
      />
      <MembersTable initial={rows} />
    </DeskShell>
  );
}
