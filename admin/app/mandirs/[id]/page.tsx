import { notFound, redirect } from "next/navigation";
import { DeskShell, GateMessage } from "@/components/desk-shell";
import { MandirForm } from "@/components/mandir-form";
import { ButtonLink, PageHeader } from "@/components/ui";
import { requireDesk } from "@/lib/auth";
import type { Mandir } from "@/lib/types";

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

  const { data } = await desk.supabase.from("mandirs").select("*").eq("id", id).maybeSingle();
  if (!data) notFound();
  const mandir = data as Mandir;

  return (
    <DeskShell email={desk.email || desk.staff.email} role={desk.staff.role}>
      <PageHeader
        title={mandir.name}
        description={mandir.city || mandir.place || mandir.id}
        actions={<ButtonLink href="/mandirs" variant="secondary">Back to list</ButtonLink>}
      />
      <MandirForm initial={{ ...mandir, next_aarti: mandir.next_aarti ?? "" }} isNew={false} />
    </DeskShell>
  );
}
