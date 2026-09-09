import { redirect } from "next/navigation";
import { DeskShell, GateMessage } from "@/components/desk-shell";
import { Card, PageHeader } from "@/components/ui";
import { requireDesk } from "@/lib/auth";

export default async function SetupPage() {
  const desk = await requireDesk();
  if (!desk.needsSetup && desk.staff) redirect("/");
  if (!desk.needsSetup && !desk.staff) {
    return <GateMessage title="No access." body="This admin already has an owner." />;
  }

  return (
    <DeskShell email={desk.email} role="setup">
      <PageHeader
        eyebrow="Setup"
        title="Catalog tables are missing"
        description="Apply the CMS migrations, then refresh this page."
      />
      <Card className="p-5 text-sm leading-6 text-zinc-600">
        Run <code className="rounded bg-zinc-100 px-1">supabase/migrations/20260910120000_catalog_cms.sql</code> and{" "}
        <code className="rounded bg-zinc-100 px-1">20260910120100_catalog_cms_policy_tighten.sql</code> in the HINVR
        SQL editor.
      </Card>
    </DeskShell>
  );
}
