import Link from "next/link";
import { Denied } from "@/components/denied";
import { DeskShell } from "@/components/desk-shell";
import { Alert, Badge, Card, PageHeader } from "@/components/ui";
import { requireStaff } from "@/lib/auth";
import { formatWhen } from "@/lib/ops";
import type { AuditRow } from "@/lib/types";

const TABLES = ["profiles", "membership_purchases", "membership_requests", "membership_prices", "staff", "mandirs", "pass_checkins"];

function changes(row: AuditRow) {
  const before = row.before ?? {};
  const after = row.after ?? {};
  const keys = [...new Set([...Object.keys(before), ...Object.keys(after)])].filter(
    (key) => JSON.stringify(before[key]) !== JSON.stringify(after[key]),
  );
  return keys.slice(0, 8).map((key) => {
    const show = (value: unknown) => (value == null || value === "" ? "—" : typeof value === "object" ? JSON.stringify(value) : String(value));
    if (!row.before) return `${key}: ${show(after[key])}`;
    if (!row.after) return `${key}: ${show(before[key])}`;
    return `${key}: ${show(before[key])} → ${show(after[key])}`;
  });
}

export default async function AuditPage({
  searchParams,
}: {
  searchParams: Promise<Record<string, string | string[] | undefined>>;
}) {
  const gate = await requireStaff("audit");
  if (!gate.ok) return <Denied reason={gate.reason} desk={gate.desk} title="Audit log" />;
  const desk = gate.desk;
  const params = await searchParams;
  const tableParam = Array.isArray(params.table) ? params.table[0] : params.table;
  const table = tableParam && TABLES.includes(tableParam) ? tableParam : "";

  let query = desk.supabase
    .from("audit_log")
    .select("id,at,actor,actor_email,action,table_name,row_id,before,after")
    .order("at", { ascending: false })
    .limit(200);
  if (table) query = query.eq("table_name", table);
  const { data, error } = await query;
  const rows = (data ?? []) as AuditRow[];

  return (
    <DeskShell email={desk.email || desk.staff.email} role={desk.staff.role}>
      <PageHeader
        title="Audit log"
        description="Membership, invoice, price, team, and check-in changes, newest first. Written by the database; nobody can edit it."
      />
      <nav className="mb-4 flex flex-wrap gap-2 text-sm" aria-label="Filter by table">
        {["", ...TABLES].map((name) => (
          <Link
            key={name || "all"}
            href={name ? `/audit?table=${name}` : "/audit"}
            aria-current={table === name ? "page" : undefined}
            className={
              table === name
                ? "rounded-md bg-zinc-900 px-3 py-2 text-white"
                : "rounded-md border border-zinc-200 bg-white px-3 py-2 text-zinc-600"
            }
          >
            {name || "All"}
          </Link>
        ))}
      </nav>
      {error ? <Alert>Couldn&apos;t load the audit log: {error.message}</Alert> : null}
      <Card className="overflow-x-auto">
        <table className="w-full min-w-[760px] text-left text-sm">
          <thead className="border-b border-zinc-200 bg-zinc-50 text-xs font-medium tracking-wide text-zinc-500 uppercase">
            <tr>
              <th className="px-4 py-3">When</th>
              <th className="px-4 py-3">Who</th>
              <th className="px-4 py-3">What</th>
              <th className="px-4 py-3">Change</th>
            </tr>
          </thead>
          <tbody>
            {rows.length === 0 ? (
              <tr>
                <td colSpan={4} className="px-4 py-10 text-center text-zinc-500">
                  Nothing recorded yet.
                </td>
              </tr>
            ) : null}
            {rows.map((row) => (
              <tr key={row.id} className="border-t border-zinc-100 align-top">
                <td className="px-4 py-3 whitespace-nowrap text-zinc-500">{formatWhen(row.at)}</td>
                <td className="px-4 py-3">{row.actor_email || (row.actor ? row.actor.slice(0, 8) : "System")}</td>
                <td className="px-4 py-3">
                  <Badge tone={row.action.startsWith("pass.") ? "red" : row.action === "delete" ? "amber" : "zinc"}>
                    {row.action}
                  </Badge>
                  <p className="mt-1 text-xs text-zinc-500">
                    {row.table_name} · {row.row_id.slice(0, 13)}
                  </p>
                </td>
                <td className="px-4 py-3">
                  <ul className="space-y-0.5 text-xs text-zinc-600">
                    {changes(row).map((line) => (
                      <li key={line} className="break-all">
                        {line}
                      </li>
                    ))}
                  </ul>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </Card>
    </DeskShell>
  );
}
