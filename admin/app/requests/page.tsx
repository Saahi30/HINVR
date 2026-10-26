import { BuyRequests } from "@/components/buy-requests";
import { CardWaitlist, type CardRequest } from "@/components/card-waitlist";
import { Denied } from "@/components/denied";
import { DeskShell } from "@/components/desk-shell";
import { RequestsTable } from "@/components/requests-table";
import { Alert, PageHeader } from "@/components/ui";
import { requireStaff } from "@/lib/auth";
import { attachMembers } from "@/lib/ops";
import { can } from "@/lib/roles";
import type { BuyRequestRow, BuyRequestStatus, DeskRequest, MemberRow } from "@/lib/types";

type ProfileLite = Pick<MemberRow, "id" | "display_name" | "city" | "phone_e164" | "tier" | "member_id">;

export default async function RequestsPage() {
  const gate = await requireStaff("requests");
  if (!gate.ok) return <Denied reason={gate.reason} desk={gate.desk} title="Requests" />;
  const desk = gate.desk;
  const db = desk.supabase;
  const handlesMembership = can(desk.staff.role, "membership");

  const [deskRes, buysRes, cardsRes] = await Promise.all([
    db
      .from("desk_requests")
      .select("id,user_id,kind,summary,city,mandir_id,status,staff_note,created_at,updated_at")
      .order("created_at", { ascending: false })
      .limit(200),
    handlesMembership
      ? db
          .from("membership_requests")
          .select("id,user_id,tier,amount_inr,status,staff_note,created_at")
          .order("created_at", { ascending: false })
          .limit(100)
      : null,
    db
      .from("physical_card_requests")
      .select("id,user_id,member_id,ship_name,ship_address,status,created_at")
      .order("created_at", { ascending: false })
      .limit(100),
  ]);

  const requests = (deskRes.data ?? []) as DeskRequest[];
  const buys = buysRes?.data ?? [];
  const ids = [...new Set([...requests.map((row) => row.user_id), ...buys.map((row) => row.user_id)])];
  const { data: profileData } = ids.length
    ? await db.from("profiles").select("id, display_name, city, phone_e164, tier, member_id").in("id", ids)
    : { data: [] };
  const profiles = (profileData ?? []) as ProfileLite[];
  const byId = new Map(profiles.map((row) => [row.id, row]));

  const buyRows: BuyRequestRow[] = buys.map((row) => {
    const profile = byId.get(row.user_id);
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
        description={
          handlesMembership
            ? "Buy requests wait here for a yes. Physical cards are a waitlist. Visit, concierge, pooja, and yatra notes sit underneath."
            : "Physical cards are a waitlist. Visit, concierge, pooja, and yatra notes sit underneath."
        }
      />
      {handlesMembership ? (
        <>
          <h2 className="mb-3 text-sm font-semibold text-zinc-900">Buy requests</h2>
          <div className="mb-10">
            {buysRes?.error ? <Alert>Couldn&apos;t load buy requests: {buysRes.error.message}</Alert> : <BuyRequests initial={buyRows} />}
          </div>
        </>
      ) : null}
      <h2 className="mb-3 text-sm font-semibold text-zinc-900">Physical cards</h2>
      <div className="mb-10">
        {cardsRes.error ? (
          <Alert>Couldn&apos;t load the card list: {cardsRes.error.message}</Alert>
        ) : (
          <CardWaitlist initial={(cardsRes.data ?? []) as CardRequest[]} />
        )}
      </div>
      <h2 className="mb-3 text-sm font-semibold text-zinc-900">Desk notes</h2>
      {deskRes.error ? (
        <Alert>Couldn&apos;t load desk requests: {deskRes.error.message}</Alert>
      ) : (
        <RequestsTable initial={attachMembers(requests, profiles)} />
      )}
    </DeskShell>
  );
}
