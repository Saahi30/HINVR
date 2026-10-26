import Link from "next/link";
import { Denied } from "@/components/denied";
import { DeskShell } from "@/components/desk-shell";
import { Badge, ButtonLink, Card, PageHeader } from "@/components/ui";
import { requireStaff } from "@/lib/auth";
import { YoutubeQuotaCard } from "@/components/youtube-quota-card";
import { attachMembers, catalogHealth, formatWhen, parseYoutubeQuota } from "@/lib/ops";
import { can } from "@/lib/roles";
import type { DeskRequest, Mandir, MemberRow } from "@/lib/types";

export default async function OverviewPage() {
  const gate = await requireStaff();
  if (!gate.ok) return <Denied reason={gate.reason} desk={gate.desk} />;
  const desk = gate.desk;
  const role = desk.staff.role;
  const seesMembers = can(role, "members");
  const db = desk.supabase;
  const count = { count: "exact" as const, head: true };

  const [mandirsRes, quotaRes, membersRes, completeRes, payingRes, openRes, requestsRes] = await Promise.all([
    db.from("mandirs").select("id,name,live,vr,published,photo_url,city,place,live_url").order("sort_order"),
    db.from("app_settings").select("value").eq("key", "youtube_quota").maybeSingle(),
    seesMembers ? db.from("profiles").select("id", count) : null,
    seesMembers ? db.from("profiles").select("id", count).eq("profile_complete", true) : null,
    seesMembers ? db.from("profiles").select("id", count).neq("tier", "None") : null,
    seesMembers ? db.from("desk_requests").select("id", count).in("status", ["new", "open"]) : null,
    seesMembers
      ? db
          .from("desk_requests")
          .select("id,user_id,kind,summary,city,mandir_id,status,staff_note,created_at")
          .order("created_at", { ascending: false })
          .limit(8)
      : null,
  ]);
  const quota = parseYoutubeQuota(quotaRes.data?.value);

  const rows = (mandirsRes.data ?? []) as Pick<
    Mandir,
    "id" | "name" | "live" | "vr" | "published" | "photo_url" | "city" | "place" | "live_url"
  >[];
  const health = catalogHealth(rows as Mandir[]);
  const memberCount = membersRes?.count ?? 0;
  const complete = completeRes?.count ?? 0;
  const paying = payingRes?.count ?? 0;
  const openRequests = openRes?.count ?? 0;
  const requests = (requestsRes?.data ?? []) as DeskRequest[];

  const profileIds = [...new Set(requests.map((row) => row.user_id))];
  const { data: profiles } = profileIds.length
    ? await db.from("profiles").select("id, display_name, city, phone_e164, tier").in("id", profileIds)
    : { data: [] };
  const recent = attachMembers(
    requests,
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
  const editsCatalog = can(role, "catalog");

  return (
    <DeskShell email={desk.email || desk.staff.email} role={role}>
      <PageHeader
        title="Overview"
        description="What the desk needs to watch: members, open requests, and catalog honesty."
        actions={
          <>
            {can(role, "requests") ? <ButtonLink href="/requests">Open inbox</ButtonLink> : null}
            {can(role, "live") ? (
              <ButtonLink href="/live" variant="secondary">
                Live board
              </ButtonLink>
            ) : null}
          </>
        }
      />
      <div className="mb-3">
        <YoutubeQuotaCard quota={quota} />
      </div>
      <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-5">
        {seesMembers ? (
          <>
            <Stat label="Members" value={memberCount} hint={`${complete} complete · ${memberCount - complete} incomplete`} />
            <Stat label="On a plan" value={paying} hint={`${memberCount - paying} with no plan`} />
            <Stat label="Open requests" value={openRequests} hint="New or open desk requests" />
          </>
        ) : null}
        <Stat label="Live flagged" value={health.live.length} hint={`${health.liveNoUrl.length} missing a URL`} />
        <Stat
          label="Catalog gaps"
          value={health.drafts.length + health.missingPhoto.length}
          hint={`${health.drafts.length} drafts · ${health.missingPhoto.length} no photo`}
        />
      </div>
      <div className="mt-6 grid gap-6 lg:grid-cols-2">
        {seesMembers ? (
          <Card className="overflow-hidden">
            <div className="flex items-center justify-between border-b border-zinc-200 px-4 py-3">
              <h2 className="text-sm font-semibold">Recent requests</h2>
              <ButtonLink href="/requests" variant="secondary">
                Inbox
              </ButtonLink>
            </div>
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
          </Card>
        ) : null}
        <Card className="overflow-hidden">
          <div className="flex items-center justify-between border-b border-zinc-200 px-4 py-3">
            <h2 className="text-sm font-semibold">Catalog issues</h2>
            {editsCatalog ? (
              <ButtonLink href="/mandirs" variant="secondary">
                Mandirs
              </ButtonLink>
            ) : null}
          </div>
          <ul className="divide-y divide-zinc-100">
            {issues.map((row) => (
              <li key={`${row.id}-${row.label}`}>
                {editsCatalog ? (
                  <Link
                    href={`/mandirs/${row.id}`}
                    className="flex items-center justify-between gap-3 px-4 py-3 hover:bg-zinc-50"
                  >
                    <IssueRow name={row.name} label={row.label} />
                  </Link>
                ) : (
                  <div className="flex items-center justify-between gap-3 px-4 py-3">
                    <IssueRow name={row.name} label={row.label} />
                  </div>
                )}
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

function IssueRow({ name, label }: { name: string; label: string }) {
  return (
    <>
      <div className="min-w-0">
        <p className="truncate text-sm font-medium">{name}</p>
        <p className="text-xs text-zinc-500">{label}</p>
      </div>
      <Badge tone={label.includes("URL") ? "red" : "amber"}>{label}</Badge>
    </>
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
