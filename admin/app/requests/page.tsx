import { redirect } from "next/navigation";
import { DeskShell, GateMessage } from "@/components/desk-shell";
import { RequestsTable } from "@/components/requests-table";
import { PageHeader } from "@/components/ui";
import { isMissingRelation, requireDesk } from "@/lib/auth";
import { attachMembers } from "@/lib/ops";
import type { DeskRequest, MemberRow } from "@/lib/types";

export default async function RequestsPage() {
  const desk = await requireDesk();
  if (desk.needsSetup) redirect("/setup");
  if (!desk.staff) {
    return <GateMessage title="No access." body="This admin already has an owner." />;
  }

  const { data, error } = await desk.supabase
    .from("desk_requests")
    .select("id,user_id,kind,summary,city,mandir_id,status,staff_note,created_at,updated_at")
    .order("created_at", { ascending: false })
    .limit(200);

  if (isMissingRelation(error)) {
    return (
      <DeskShell email={desk.email || desk.staff.email} role={desk.staff.role}>
        <PageHeader
          title="Requests"
          description="Visit assist, concierge, pooja, and yatra waitlists from the phone."
        />
        <p className="rounded-xl border border-zinc-200 bg-white px-4 py-8 text-center text-sm text-zinc-500">
          Apply <code className="rounded bg-zinc-100 px-1">supabase/migrations/20260925120000_desk_ops.sql</code> in the
          HINVR SQL editor, then refresh.
        </p>
      </DeskShell>
    );
  }

  const requests = (data ?? []) as DeskRequest[];
  const ids = [...new Set(requests.map((row) => row.user_id))];
  let profiles: Pick<MemberRow, "id" | "display_name" | "city" | "phone_e164" | "tier">[] = [];
  if (ids.length) {
    const withPhone = await desk.supabase
      .from("profiles")
      .select("id, display_name, city, phone_e164, tier")
      .in("id", ids);
    if (withPhone.error) {
      const fallback = await desk.supabase.from("profiles").select("id, display_name, city, tier").in("id", ids);
      profiles = ((fallback.data ?? []) as Pick<MemberRow, "id" | "display_name" | "city" | "tier">[]).map((row) => ({
        ...row,
        phone_e164: "",
      }));
    } else {
      profiles = (withPhone.data ?? []) as Pick<MemberRow, "id" | "display_name" | "city" | "phone_e164" | "tier">[];
    }
  }

  return (
    <DeskShell email={desk.email || desk.staff.email} role={desk.staff.role}>
      <PageHeader
        title="Requests"
        description="Visit assist, concierge, pooja, and yatra waitlists from the phone. New rows land here as soon as Cloud is on."
      />
      <RequestsTable initial={attachMembers(requests, profiles)} />
    </DeskShell>
  );
}
