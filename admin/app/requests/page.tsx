import { redirect } from "next/navigation";
import { BuyRequests } from "@/components/buy-requests";
import { CardWaitlist, type CardRequest } from "@/components/card-waitlist";
import { DeskShell, GateMessage } from "@/components/desk-shell";
import { RequestsTable } from "@/components/requests-table";
import { PageHeader } from "@/components/ui";
import { isMissingRelation, requireDesk } from "@/lib/auth";
import { attachMembers } from "@/lib/ops";
import type { BuyRequestRow, BuyRequestStatus, DeskRequest, MemberRow } from "@/lib/types";

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

  const buys = await desk.supabase
    .from("membership_requests")
    .select("id,user_id,tier,amount_inr,status,staff_note,created_at")
    .order("created_at", { ascending: false })
    .limit(100);
  const memberIdRows = await desk.supabase.from("profiles").select("member_id");
  const invoiceRows = await desk.supabase.from("membership_purchases").select("invoice_number");
  const cards = await desk.supabase
    .from("physical_card_requests")
    .select("id,user_id,member_id,ship_name,ship_address,status,created_at")
    .order("created_at", { ascending: false })
    .limit(100);

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

  const buyIds = [...new Set((buys.data ?? []).map((row) => row.user_id))];
  let buyProfiles: Pick<MemberRow, "id" | "display_name" | "city" | "phone_e164" | "tier" | "member_id">[] = [];
  if (buyIds.length) {
    const withPhone = await desk.supabase
      .from("profiles")
      .select("id, display_name, city, phone_e164, tier, member_id")
      .in("id", buyIds);
    buyProfiles = (withPhone.data ?? []) as typeof buyProfiles;
  }
  const buyById = new Map(buyProfiles.map((row) => [row.id, row]));
  const buyRows: BuyRequestRow[] = (buys.data ?? []).map((row) => {
    const profile = buyById.get(row.user_id);
    return {
      id: row.id,
      user_id: row.user_id,
      tier: row.tier,
      amount_inr: row.amount_inr,
      status: row.status as BuyRequestStatus,
      staff_note: row.staff_note,
      created_at: row.created_at,
      display_name: profile?.display_name || "Unnamed",
      city: profile?.city || "",
      phone_e164: profile?.phone_e164 || "",
      member_id: profile?.member_id || "",
      current_tier: profile?.tier || "None",
    };
  });

  return (
    <DeskShell email={desk.email || desk.staff.email} role={desk.staff.role}>
      <PageHeader
        title="Requests"
        description="Buy requests wait here for a yes. Physical cards are a waitlist. Visit, concierge, pooja, and yatra notes sit underneath."
      />
      <h2 className="mb-3 text-sm font-semibold text-zinc-900">Buy requests</h2>
      {isMissingRelation(buys.error) ? (
        <p className="mb-8 rounded-xl border border-zinc-200 bg-white px-4 py-8 text-center text-sm text-zinc-500">
          Apply <code className="rounded bg-zinc-100 px-1">supabase/migrations/20260927210000_membership_requests_and_checkins.sql</code>{" "}
          in the HINVR SQL editor, then refresh.
        </p>
      ) : (
        <div className="mb-10">
          <BuyRequests
            initial={buyRows}
            memberIds={(memberIdRows.data ?? []).map((row) => row.member_id).filter(Boolean)}
            invoiceNumbers={(invoiceRows.data ?? []).map((row) => row.invoice_number).filter(Boolean)}
          />
        </div>
      )}
      <h2 className="mb-3 text-sm font-semibold text-zinc-900">Physical cards</h2>
      {isMissingRelation(cards.error) ? (
        <p className="mb-10 rounded-xl border border-zinc-200 bg-white px-4 py-8 text-center text-sm text-zinc-500">
          Apply{" "}
          <code className="rounded bg-zinc-100 px-1">supabase/migrations/20260928120000_pass_credentials.sql</code> in
          the HINVR SQL editor, then refresh.
        </p>
      ) : cards.error ? (
        <p className="mb-10 text-sm text-red-600">{cards.error.message}</p>
      ) : (
        <div className="mb-10">
          <CardWaitlist initial={(cards.data ?? []) as CardRequest[]} />
        </div>
      )}
      <h2 className="mb-3 text-sm font-semibold text-zinc-900">Desk notes</h2>
      <RequestsTable initial={attachMembers(requests, profiles)} />
    </DeskShell>
  );
}
