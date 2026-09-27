import { redirect } from "next/navigation";
import { DeskShell, GateMessage } from "@/components/desk-shell";
import { MembersTable } from "@/components/members-table";
import { ButtonLink, PageHeader } from "@/components/ui";
import { requireDesk } from "@/lib/auth";
import type { MemberRow } from "@/lib/types";

export default async function MembersPage() {
  const desk = await requireDesk();
  if (desk.needsSetup) redirect("/setup");
  if (!desk.staff) {
    return <GateMessage title="No access." body="This admin already has an owner." />;
  }

  const full = await desk.supabase
    .from("profiles")
    .select("id, display_name, city, addresses, tier, member_id, valid_until, audience, profile_complete, phone_e164, updated_at")
    .order("display_name");

  let rows = (full.data ?? []) as MemberRow[];
  const purchases = await desk.supabase
    .from("membership_purchases")
    .select("id, user_id, amount_inr, invoice_number, purchased_on, valid_from, valid_until")
    .order("purchased_on", { ascending: false });
  const latestPurchase = new Map<string, NonNullable<typeof purchases.data>[number]>();
  for (const purchase of purchases.data ?? []) {
    if (!latestPurchase.has(purchase.user_id)) latestPurchase.set(purchase.user_id, purchase);
  }
  if (full.error) {
    const fallback = await desk.supabase
      .from("profiles")
      .select("id, display_name, city, tier, member_id, valid_until, audience")
      .order("display_name");
    rows = ((fallback.data ?? []) as Omit<MemberRow, "profile_complete" | "phone_e164" | "addresses">[]).map((row) => ({
      ...row,
      profile_complete: false,
      phone_e164: "",
      addresses: [],
    }));
  }
  rows = rows.map((row) => {
    const purchase = latestPurchase.get(row.id);
    if (!purchase) return row;
    return {
      ...row,
      purchase_id: purchase.id,
      invoice_number: purchase.invoice_number,
      amount_inr: purchase.amount_inr,
      purchased_on: purchase.purchased_on,
      valid_from: purchase.valid_from,
      invoice_valid_until: purchase.valid_until,
    };
  });

  return (
    <DeskShell email={desk.email || desk.staff.email} role={desk.staff.role}>
      <PageHeader
        title="Members"
        description="Plans are confirmed from buy requests. Use this table to correct a member ID, a date, or an invoice."
        actions={<ButtonLink href="/requests" variant="secondary">Buy requests</ButtonLink>}
      />
      <MembersTable initial={rows} />
    </DeskShell>
  );
}
