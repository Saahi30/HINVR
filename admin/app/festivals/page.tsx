import { Denied } from "@/components/denied";
import { DeskShell } from "@/components/desk-shell";
import { FestivalsTable } from "@/components/festivals-table";
import { PageHeader } from "@/components/ui";
import { requireStaff } from "@/lib/auth";
import { can } from "@/lib/roles";
import type { Festival } from "@/lib/types";

export default async function FestivalsPage() {
  const gate = await requireStaff("catalog");
  if (!gate.ok) return <Denied reason={gate.reason} desk={gate.desk} />;
  const desk = gate.desk;

  const { data } = await desk.supabase.from("festivals").select("*").order("starts_on");

  return (
    <DeskShell email={desk.email || desk.staff.email} role={desk.staff.role}>
      <PageHeader
        title="Festivals"
        description="While today (India time) falls in an enabled festival's dates, the headset adds its puja to the sanctum. Check each date against a panchang before enabling it."
      />
      <FestivalsTable
        initial={(data ?? []) as Festival[]}
        canDelete={can(desk.staff.role, "deleteMandir")}
      />
    </DeskShell>
  );
}
