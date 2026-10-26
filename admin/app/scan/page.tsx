import { Denied } from "@/components/denied";
import { DeskShell } from "@/components/desk-shell";
import { PassScanner } from "@/components/pass-scanner";
import { PageHeader } from "@/components/ui";
import { requireStaff } from "@/lib/auth";

export default async function ScanPage() {
  const gate = await requireStaff("scan");
  if (!gate.ok) return <Denied reason={gate.reason} desk={gate.desk} />;
  const desk = gate.desk;

  return (
    <DeskShell email={desk.email || desk.staff.email} role={desk.staff.role}>
      <PageHeader
        title="Scan pass"
        description="Scan a signed pass. A copied string is refused. The code expires on its own, and a new code from the phone retires the old one."
      />
      <PassScanner />
    </DeskShell>
  );
}
