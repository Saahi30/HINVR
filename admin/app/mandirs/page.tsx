import { Denied } from "@/components/denied";
import { DeskShell } from "@/components/desk-shell";
import { MandirsTable } from "@/components/mandirs-table";
import { ButtonLink, PageHeader } from "@/components/ui";
import { requireStaff } from "@/lib/auth";
import { can } from "@/lib/roles";
import type { Mandir } from "@/lib/types";

export default async function MandirsPage() {
  const gate = await requireStaff("catalog");
  if (!gate.ok) return <Denied reason={gate.reason} desk={gate.desk} />;
  const desk = gate.desk;

  const { data } = await desk.supabase
    .from("mandirs")
    .select(
      "id,name,place,city,scene,photo_url,live,vr,pass_accepted,next_aarti,timings,updated_label,live_url,vr_url,deity,summary,history,significance,architecture,dress_code,best_time,visitor_notes,facilities,address,official_website,contact_phone,sort_order,published,updated_at",
    )
    .order("sort_order");

  return (
    <DeskShell email={desk.email || desk.staff.email} role={desk.staff.role}>
      <PageHeader
        title="Mandirs"
        description="Add, unpublish, or delete temples. Unpublished rows stay hidden in the app."
        actions={<ButtonLink href="/mandirs/new">Add mandir</ButtonLink>}
      />
      <MandirsTable initial={(data ?? []) as Mandir[]} canDelete={can(desk.staff.role, "deleteMandir")} />
    </DeskShell>
  );
}
