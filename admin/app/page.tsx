import Link from "next/link";
import { redirect } from "next/navigation";
import { DeskShell, GateMessage } from "@/components/desk-shell";
import { Badge, ButtonLink, Card, PageHeader } from "@/components/ui";
import { isMissingRelation, requireDesk } from "@/lib/auth";
import { attachMembers, catalogHealth, formatWhen, isPaying } from "@/lib/ops";
import type { DeskRequest, Mandir, MemberRow } from "@/lib/types";

export default async function OverviewPage() {
  const desk = await requireDesk();
  if (desk.needsSetup) redirect("/setup");
  if (!desk.staff) {
    return (
      <GateMessage
        title="This admin is already claimed."
        body="Ask the owner to add your email. The first account to sign up becomes owner."
      />
    );
  }

  const [mandirsRes, membersRes, requestsRes] = await Promise.all([
    desk.supabase
      .from("mandirs")
      .select("id,name,live,vr,published,photo_url,city,place,live_url")
      .order("sort_order"),
    desk.supabase.from("profiles").select("id, profile_complete, tier"),
    desk.supabase
      .from("desk_requests")
      .select("id,user_id,kind,summary,city,mandir_id,status,staff_note,created_at")
      .order("created_at", { ascending: false })
      .limit(40),
  ]);

  const rows = (mandirsRes.data ?? []) as Pick<
    Mandir,
    "id" | "name" | "live" | "vr" | "published" | "photo_url" | "city" | "place" | "live_url"
  >[];
  const members = (membersRes.data ?? []) as Pick<MemberRow, "id" | "profile_complete" | "tier">[];
  const requestsMissing = isMissingRelation(requestsRes.error);
  const requests = requestsMissing ? [] : ((requestsRes.data ?? []) as DeskRequest[]);
  const health = catalogHealth(rows as Mandir[]);
  const complete = members.filter((row) => row.profile_complete).length;
  const incomplete = members.length - complete;
  const paying = members.filter((row) => isPaying(row.tier)).length;
  const openRequests = requests.filter((row) => row.status === "new" || row.status === "open").length;

  const profileIds = [...new Set(requests.slice(0, 8).map((row) => row.user_id))];
  const { data: profiles } = profileIds.length
    ? await desk.supabase
        .from("profiles")
        .select("id, display_name, city, phone_e164, tier")
        .in("id", profileIds)
    : { data: [] };
  const recent = attachMembers(
    requests.slice(0, 8),
    (profiles ?? []) as Pick<MemberRow, "id" | "display_name" | "city" | "phone_e164" | "tier">[],
  );

  const issues = [
    ...health.liveNoUrl.map((row) => ({ id: row.id, name: row.name, label: "Live, no URL" })),
    ...health.liveDraft.map((row) => ({ id: row.id, name: row.name, label: "Live, still a draft" })),
    ...health.missingPhoto
      .filter((row) => !row.live)
      .slice(0, 6)
      .map((row) => ({ id: row.id, name: row.name, label: "Missing photo" })),
  ].slice(0, 8);

  return (
    <DeskShell email={desk.email || desk.staff.email} role={desk.staff.role}>
      <PageHeader
        title="Overview"
        description="What the desk needs to watch: members, open requests, and catalog honesty."
        actions={
          <>
            <ButtonLink href="/requests">Open inbox</ButtonLink>
            <ButtonLink href="/live" variant="secondary">
              Live board
            </ButtonLink>
          </>
        }
      />
      <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-5">
        <Stat
          label="Members"
          value={members.length}
          hint={`${complete} complete · ${incomplete} incomplete`}
        />
        <Stat label="Paying" value={paying} hint={`${members.length - paying} on None`} />
        <Stat
          label="Open requests"
          value={requestsMissing ? 0 : openRequests}
          hint={requestsMissing ? "Apply desk_ops SQL" : `${requests.length} total fetched`}
        />
        <Stat
          label="Live flagged"
          value={health.live.length}
          hint={`${health.liveNoUrl.length} missing a URL`}
        />
        <Stat
          label="Catalog gaps"
          value={health.drafts.length + health.missingPhoto.length}
          hint={`${health.drafts.length} drafts · ${health.missingPhoto.length} no photo`}
        />
      </div>
      <div className="mt-6 grid gap-6 lg:grid-cols-2">
        <Card className="overflow-hidden">
          <div className="flex items-center justify-between border-b border-zinc-200 px-4 py-3">
            <h2 className="text-sm font-semibold">Recent requests</h2>
            <ButtonLink href="/requests" variant="secondary">
              Inbox
            </ButtonLink>
          </div>
          {requestsMissing ? (
            <p className="px-4 py-10 text-center text-sm text-zinc-500">
              Run <code className="rounded bg-zinc-100 px-1">supabase/migrations/20260925120000_desk_ops.sql</code> to
              see visit, concierge, pooja, and yatra demand.
            </p>
          ) : (
            <ul className="divide-y divide-zinc-100">
              {recent.map((row) => (
                <li key={row.id} className="flex items-start justify-between gap-3 px-4 py-3">
                  <div className="min-w-0">
                    <p className="truncate text-sm font-medium">{row.display_name}</p>
                    <p className="truncate text-xs text-zinc-500">
                      {row.kind} · {row.summary}
                    </p>
                  </div>
                  <div className="flex shrink-0 flex-col items-end gap-1">
                    <Badge tone={row.status === "new" ? "amber" : row.status === "open" ? "blue" : "green"}>
                      {row.status}
                    </Badge>
                    <span className="text-[11px] text-zinc-400">{formatWhen(row.created_at)}</span>
                  </div>
                </li>
              ))}
              {recent.length === 0 ? (
                <li className="px-4 py-10 text-center text-sm text-zinc-500">
                  No desk requests yet. They appear when someone sends from the phone.
                </li>
              ) : null}
            </ul>
          )}
        </Card>
        <Card className="overflow-hidden">
          <div className="flex items-center justify-between border-b border-zinc-200 px-4 py-3">
            <h2 className="text-sm font-semibold">Catalog issues</h2>
            <ButtonLink href="/mandirs" variant="secondary">
              Mandirs
            </ButtonLink>
          </div>
          <ul className="divide-y divide-zinc-100">
            {issues.map((row) => (
              <li key={`${row.id}-${row.label}`}>
                <Link href={`/mandirs/${row.id}`} className="flex items-center justify-between gap-3 px-4 py-3 hover:bg-zinc-50">
                  <div className="min-w-0">
                    <p className="truncate text-sm font-medium">{row.name}</p>
                    <p className="text-xs text-zinc-500">{row.label}</p>
                  </div>
                  <Badge tone={row.label.includes("URL") ? "red" : "amber"}>{row.label}</Badge>
                </Link>
              </li>
            ))}
            {issues.length === 0 ? (
              <li className="px-4 py-10 text-center text-sm text-zinc-500">No live-URL or photo gaps right now.</li>
            ) : null}
          </ul>
        </Card>
      </div>
    </DeskShell>
  );
}

function Stat({ label, value, hint }: { label: string; value: number; hint: string }) {
  return (
    <Card className="px-4 py-3">
      <p className="text-xs font-medium uppercase tracking-wide text-zinc-500">{label}</p>
      <p className="mt-1 text-2xl font-semibold tabular-nums">{value}</p>
      <p className="mt-1 text-xs text-zinc-500">{hint}</p>
    </Card>
  );
}
