import { notFound, redirect } from "next/navigation";
import { DeskShell, GateMessage } from "@/components/desk-shell";
import { LiveSources } from "@/components/live-sources";
import { MandirForm } from "@/components/mandir-form";
import { ButtonLink, PageHeader } from "@/components/ui";
import { requireDesk } from "@/lib/auth";
import type { LiveSource, Mandir } from "@/lib/types";

export default async function EditMandirPage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const { id } = await params;
  const desk = await requireDesk();
  if (desk.needsSetup) redirect("/setup");
  if (!desk.staff) {
    return <GateMessage title="No access." body="This admin already has an owner." />;
  }

  const [{ data }, { data: sourceRows }] = await Promise.all([
    desk.supabase.from("mandirs").select("*").eq("id", id).maybeSingle(),
    desk.supabase.from("live_sources").select("*").eq("mandir_id", id).order("priority"),
  ]);
  if (!data) notFound();
  const mandir = data as Mandir;
  const sources = (sourceRows ?? []) as LiveSource[];
  const managed = sources.some((row) => row.enabled);

  return (
    <DeskShell email={desk.email || desk.staff.email} role={desk.staff.role}>
      <PageHeader
        title={mandir.name}
        description={mandir.city || mandir.place || mandir.id}
        actions={<ButtonLink href="/mandirs" variant="secondary">Back to list</ButtonLink>}
      />
      <MandirForm initial={{ ...mandir, next_aarti: mandir.next_aarti ?? "" }} isNew={false} liveManaged={managed} />
      <LiveSources mandirId={mandir.id} initial={sources} />
    </DeskShell>
  );
}
