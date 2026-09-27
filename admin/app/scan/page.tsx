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
        description="Read a member QR, confirm who they are, and check them in. The visit appears on their phone under Pass."
      />
      <PassScanner />
    </DeskShell>
  );
}
