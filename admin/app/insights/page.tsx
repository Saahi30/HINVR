import { Denied } from "@/components/denied";
import { DeskShell } from "@/components/desk-shell";
import { InsightsView, PeriodSwitch } from "@/components/insights-view";
import { Alert, Card, PageHeader } from "@/components/ui";
import { requireStaff } from "@/lib/auth";
import { periodFrom } from "@/lib/insights";
import { can } from "@/lib/roles";
import type { Insights } from "@/lib/types";

export default async function InsightsPage({
  searchParams,
}: {
  searchParams: Promise<Record<string, string | string[] | undefined>>;
}) {
  const gate = await requireStaff("insights");
  if (!gate.ok) return <Denied reason={gate.reason} desk={gate.desk} />;
  const desk = gate.desk;
  const days = periodFrom((await searchParams).days);

  // One round trip, computed from what is already stored; nothing runs until this page opens.
  const { data, error } = await desk.supabase.rpc("staff_insights", { p_days: days });
  const missing = error && /staff_insights|could not find the function|PGRST202/i.test(`${error.code} ${error.message}`);

  return (
    <DeskShell email={desk.email || desk.staff.email} role={desk.staff.role}>
      <PageHeader
        title="Insights"
        description="How the sanctum and View in VR are used: offerings, headsets, the darshan windows members choose, and crashes. Counts only; no member is named here."
        actions={<PeriodSwitch current={days} />}
      />
      {error ? (
        <Card className="px-4 py-4">
          <Alert>
            {missing
              ? "Insights need the migration 20261027170000_insights_and_crashes.sql. Apply it, then reload."
              : `Couldn't load insights: ${error.message}`}
          </Alert>
        </Card>
      ) : (
        <InsightsView data={data as Insights} seesCrashes={can(desk.staff.role, "crashes")} />
      )}
    </DeskShell>
  );
}
