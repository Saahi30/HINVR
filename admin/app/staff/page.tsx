import { Denied } from "@/components/denied";
import { DeskShell } from "@/components/desk-shell";
import { StaffTable } from "@/components/staff-table";
import { PageHeader } from "@/components/ui";
import { requireStaff } from "@/lib/auth";
import type { Staff } from "@/lib/types";

export default async function StaffPage() {
  const gate = await requireStaff();
  if (!gate.ok) return <Denied reason={gate.reason} desk={gate.desk} />;
  const desk = gate.desk;

  const { data } = await desk.supabase.from("staff").select("user_id, email, role").order("created_at");

  return (
    <DeskShell email={desk.email || desk.staff.email} role={desk.staff.role}>
      <PageHeader
        title="Team"
        description="Each person gets one role. Only an owner can add people or change roles. They must create an account first."
      />
      <StaffTable initial={(data ?? []) as Staff[]} me={desk.staff.user_id} isOwner={desk.staff.role === "owner"} />
    </DeskShell>
  );
}
