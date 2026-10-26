import Link from "next/link";
import { redirect } from "next/navigation";
import { DeskShell, GateMessage } from "@/components/desk-shell";
import { Card, PageHeader } from "@/components/ui";
import type { Desk } from "@/lib/auth";
import { ROLE_LABELS } from "@/lib/roles";

/** What a page shows when requireStaff() says no. */
export function Denied({ reason, desk, title }: { reason: "setup" | "not-staff" | "forbidden"; desk: Desk; title?: string }) {
  if (reason === "setup") redirect("/setup");
  if (reason === "not-staff" || !desk.staff) {
    return (
      <GateMessage
        title="No access yet."
        body="This account is not on the desk team. Ask an owner to add your email."
        action={
          <Link href="/setup" className="text-sm font-medium text-zinc-900 underline">
            Setting up this admin for the first time?
          </Link>
        }
      />
    );
  }
  return (
    <DeskShell email={desk.email || desk.staff.email} role={desk.staff.role}>
      <PageHeader title={title ?? "Not for your role"} />
      <Card className="p-6 text-sm text-zinc-600">
        {ROLE_LABELS[desk.staff.role]} accounts can&apos;t open this page. Ask an owner if you need it.
      </Card>
    </DeskShell>
  );
}
