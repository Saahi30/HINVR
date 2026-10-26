import { notFound } from "next/navigation";
import { Denied } from "@/components/denied";
import { DeskShell } from "@/components/desk-shell";
import { LiveSources } from "@/components/live-sources";
import { MandirForm } from "@/components/mandir-form";
import { ButtonLink, PageHeader } from "@/components/ui";
import { requireStaff } from "@/lib/auth";
import { can } from "@/lib/roles";
import type { LiveSource, Mandir } from "@/lib/types";

export default async function EditMandirPage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const { id } = await params;
  const gate = await requireStaff("catalog");
  if (!gate.ok) return <Denied reason={gate.reason} desk={gate.desk} />;
  const desk = gate.desk;

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
      <MandirForm
        initial={{ ...mandir, next_aarti: mandir.next_aarti ?? "" }}
        isNew={false}
        liveManaged={managed}
        canDelete={can(desk.staff.role, "deleteMandir")}
      />
      <LiveSources mandirId={mandir.id} initial={sources} />
    </DeskShell>
  );
}
