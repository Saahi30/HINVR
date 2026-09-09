import { redirect } from "next/navigation";
import { DeskShell, GateMessage } from "@/components/desk-shell";
import { StaffTable } from "@/components/staff-table";
import { PageHeader } from "@/components/ui";
import { requireDesk } from "@/lib/auth";
import type { Staff } from "@/lib/types";

export default async function StaffPage() {
  const desk = await requireDesk();
  if (desk.needsSetup) redirect("/setup");
  if (!desk.staff) {
    return <GateMessage title="No access." body="This admin already has an owner." />;
  }

  const { data } = await desk.supabase.from("staff").select("user_id, email, role").order("created_at");

  return (
    <DeskShell email={desk.email || desk.staff.email} role={desk.staff.role}>
      <PageHeader
        title="Team"
        description="Editors can change the catalog. Only the owner can invite or remove people. They must create an account first."
      />
      <StaffTable
        initial={(data ?? []) as Staff[]}
        me={desk.staff.user_id}
        isOwner={desk.staff.role === "owner"}
      />
    </DeskShell>
  );
}
