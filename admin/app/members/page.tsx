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

  const { data } = await desk.supabase
    .from("profiles")
    .select("id, display_name, city, tier, member_id, valid_until, audience")
    .order("display_name");

  return (
    <DeskShell email={desk.email || desk.staff.email} role={desk.staff.role}>
      <PageHeader
        title="Members"
        description="Set tier and pass dates. People appear after they finish profile setup on the phone."
      />
      <MembersTable initial={(data ?? []) as MemberRow[]} />
    </DeskShell>
  );
}
