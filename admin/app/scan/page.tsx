import { redirect } from "next/navigation";
import { DeskShell, GateMessage } from "@/components/desk-shell";
import { PassScanner } from "@/components/pass-scanner";
import { PageHeader } from "@/components/ui";
import { requireDesk } from "@/lib/auth";

export default async function ScanPage() {
  const desk = await requireDesk();
  if (desk.needsSetup) redirect("/setup");
  if (!desk.staff) {
    return <GateMessage title="No access." body="This admin already has an owner." />;
  }

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
