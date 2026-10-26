import { Denied } from "@/components/denied";
import { DeskShell } from "@/components/desk-shell";
import { MembersTable, type MemberFilters } from "@/components/members-table";
import { Alert, ButtonLink, PageHeader } from "@/components/ui";
import { requireStaff } from "@/lib/auth";
import { can } from "@/lib/roles";
import { TIERS, type MemberRow } from "@/lib/types";

const PAGE_SIZE = 50;
const COLUMNS =
  "id, display_name, city, addresses, tier, member_id, valid_until, audience, profile_complete, phone_e164, updated_at";

function first(value: string | string[] | undefined) {
  return (Array.isArray(value) ? value[0] : value) ?? "";
}

/** Characters that would break a PostgREST or() filter are dropped. */
function cleanQuery(value: string) {
  return value.replace(/[,()*%\\:"']/g, " ").replace(/\s+/g, " ").trim().slice(0, 60);
}

export default async function MembersPage({
  searchParams,
}: {
  searchParams: Promise<Record<string, string | string[] | undefined>>;
}) {
  const gate = await requireStaff("members");
  if (!gate.ok) return <Denied reason={gate.reason} desk={gate.desk} title="Members" />;
  const desk = gate.desk;
  const canEdit = can(desk.staff.role, "membership");

  const params = await searchParams;
  const q = cleanQuery(first(params.q));
  const tierParam = first(params.tier);
  const tier = (TIERS as readonly string[]).includes(tierParam) ? tierParam : "all";
  const funnelParam = first(params.funnel);
  const funnel: MemberFilters["funnel"] = funnelParam === "complete" || funnelParam === "incomplete" ? funnelParam : "all";
  const page = Math.max(1, Math.min(10_000, Number.parseInt(first(params.page), 10) || 1));
  const from = (page - 1) * PAGE_SIZE;

  let invoiceUserIds: string[] = [];
  if (q && canEdit && /^inv/i.test(q)) {
    const { data } = await desk.supabase
      .from("membership_purchases")
      .select("user_id")
      .ilike("invoice_number", `%${q}%`)
      .limit(50);
    invoiceUserIds = [...new Set((data ?? []).map((row) => row.user_id).filter(Boolean) as string[])];
  }

  let query = desk.supabase.from("profiles").select(COLUMNS, { count: "exact" });
  if (q) {
    const pattern = `%${q}%`;
    const clauses = [
      `display_name.ilike.${pattern}`,
      `city.ilike.${pattern}`,
      `member_id.ilike.${pattern}`,
      `phone_e164.ilike.${pattern}`,
    ];
    if (invoiceUserIds.length) clauses.push(`id.in.(${invoiceUserIds.join(",")})`);
    query = query.or(clauses.join(","));
  }
  if (tier !== "all") query = query.eq("tier", tier);
  if (funnel === "complete") query = query.eq("profile_complete", true);
  if (funnel === "incomplete") query = query.eq("profile_complete", false);
  const { data, error, count } = await query
    .order("display_name")
    .order("id")
    .range(from, from + PAGE_SIZE - 1);

  let rows = (data ?? []) as MemberRow[];
  if (canEdit && rows.length) {
    const { data: purchases } = await desk.supabase
      .from("membership_purchases")
      .select("id, user_id, amount_inr, invoice_number, purchased_on, valid_from, valid_until, created_at")
      .in(
        "user_id",
        rows.map((row) => row.id),
      )
      .order("purchased_on", { ascending: false })
      .order("created_at", { ascending: false });
    const latest = new Map<string, NonNullable<typeof purchases>[number]>();
    for (const purchase of purchases ?? []) {
      if (purchase.user_id && !latest.has(purchase.user_id)) latest.set(purchase.user_id, purchase);
    }
    rows = rows.map((row) => {
      const purchase = latest.get(row.id);
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
  }

  return (
    <DeskShell email={desk.email || desk.staff.email} role={desk.staff.role}>
      <PageHeader
        title="Members"
        description={
          canEdit
            ? "Plans are confirmed from buy requests. Use Edit to correct a member ID, a date, or an invoice."
            : "Member contact details for the desk. Plans are changed by owner, admin, or finance."
        }
        actions={
          can(desk.staff.role, "requests") ? (
            <ButtonLink href="/requests" variant="secondary">
              Buy requests
            </ButtonLink>
          ) : null
        }
      />
      {error ? <Alert>Couldn&apos;t load members: {error.message}</Alert> : null}
      <MembersTable
        rows={rows}
        total={count ?? 0}
        page={page}
        pageSize={PAGE_SIZE}
        filters={{ q, tier, funnel }}
        canEdit={canEdit}
      />
    </DeskShell>
  );
}
