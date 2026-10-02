import Link from "next/link";
import { redirect } from "next/navigation";
import { DeskShell, GateMessage } from "@/components/desk-shell";
import { LiveCheckButton } from "@/components/live-check-button";
import { YoutubeQuotaCard } from "@/components/youtube-quota-card";
import { Badge, ButtonLink, Card, PageHeader } from "@/components/ui";
import { requireDesk } from "@/lib/auth";
import { catalogHealth, formatWhen, parseYoutubeQuota } from "@/lib/ops";
import type { LiveSource, Mandir } from "@/lib/types";

type SourceLite = Pick<
  LiveSource,
  "mandir_id" | "channel_name" | "enabled" | "last_live" | "last_viewers" | "last_error" | "official"
>;

export default async function LivePage() {
  const desk = await requireDesk();
  if (desk.needsSetup) redirect("/setup");
  if (!desk.staff) {
    return <GateMessage title="No access." body="This admin already has an owner." />;
  }

  const [{ data }, { data: sourceData }, { data: quotaRow }] = await Promise.all([
    desk.supabase
      .from("mandirs")
      .select("id,name,place,city,photo_url,live,published,live_url,updated_label,updated_at,live_checked_at")
      .order("sort_order"),
    desk.supabase
      .from("live_sources")
      .select("mandir_id,channel_name,enabled,last_live,last_viewers,last_error,official"),
    desk.supabase.from("app_settings").select("value").eq("key", "youtube_quota").maybeSingle(),
  ]);
  const quota = parseYoutubeQuota(quotaRow?.value);

  const mandirs = (data ?? []) as Pick<
    Mandir,
    | "id"
    | "name"
    | "place"
    | "city"
    | "photo_url"
    | "live"
    | "published"
    | "live_url"
    | "updated_label"
    | "updated_at"
    | "live_checked_at"
  >[];
  const sources = ((sourceData ?? []) as SourceLite[]).filter((row) => row.enabled);
  const sourcesFor = (id: string) => sources.filter((row) => row.mandir_id === id);
  const broken = sources.filter((row) => row.last_error);
  const lastCheck = mandirs
    .map((row) => row.live_checked_at)
    .filter((value): value is string => Boolean(value))
    .sort()
    .at(-1);

  const health = catalogHealth(mandirs as Mandir[]);
  const liveRows = [...health.live].sort((a, b) => {
    const aBad = Number(!a.live_url?.trim()) + Number(!a.published);
    const bBad = Number(!b.live_url?.trim()) + Number(!b.published);
    return bBad - aBad;
  });
  const photoGaps = health.missingPhoto.filter((row) => !row.live);

  return (
    <DeskShell email={desk.email || desk.staff.email} role={desk.staff.role}>
      <PageHeader
        title="Live"
        description="Mandirs with live sources are checked against YouTube every 10 minutes. The rest stay as set by hand."
        actions={
          <>
            <LiveCheckButton />
            <ButtonLink href="/mandirs">Edit mandirs</ButtonLink>
          </>
        }
      />
      <div className="mb-3">
        <YoutubeQuotaCard quota={quota} />
      </div>
      <div className="mb-6 grid gap-3 sm:grid-cols-4">
        <Card className="px-4 py-3">
          <p className="text-xs font-medium uppercase tracking-wide text-zinc-500">Live now</p>
          <p className="mt-1 text-2xl font-semibold tabular-nums">{health.live.length}</p>
        </Card>
        <Card className="px-4 py-3">
          <p className="text-xs font-medium uppercase tracking-wide text-zinc-500">Last check</p>
          <p className="mt-1 text-2xl font-semibold tabular-nums">{lastCheck ? formatWhen(lastCheck) : "Never"}</p>
        </Card>
        <Card className="px-4 py-3">
          <p className="text-xs font-medium uppercase tracking-wide text-zinc-500">Broken sources</p>
          <p className="mt-1 text-2xl font-semibold tabular-nums">{broken.length}</p>
        </Card>
        <Card className="px-4 py-3">
          <p className="text-xs font-medium uppercase tracking-wide text-zinc-500">Live drafts</p>
          <p className="mt-1 text-2xl font-semibold tabular-nums">{health.liveDraft.length}</p>
        </Card>
      </div>
      <Card className="overflow-hidden">
        <div className="border-b border-zinc-200 px-4 py-3">
          <h2 className="text-sm font-semibold">Live now</h2>
        </div>
        <ul className="divide-y divide-zinc-100">
          {liveRows.map((row) => {
            const noUrl = !row.live_url?.trim();
            const noPhoto = !row.photo_url?.trim();
            const feed = sourcesFor(row.id)
              .filter((source) => source.last_live)
              .sort((a, b) => Number(b.official) - Number(a.official) || (b.last_viewers ?? 0) - (a.last_viewers ?? 0))[0];
            return (
              <li key={row.id}>
                <Link href={`/mandirs/${row.id}`} className="flex items-center gap-3 px-4 py-3 hover:bg-zinc-50">
                  {row.photo_url ? (
                    // eslint-disable-next-line @next/next/no-img-element
                    <img src={row.photo_url} alt="" className="h-10 w-10 rounded-md object-cover" />
                  ) : (
                    <div className="h-10 w-10 rounded-md bg-zinc-100" />
                  )}
                  <div className="min-w-0 flex-1">
                    <p className="truncate text-sm font-medium">{row.name}</p>
                    <p className="truncate text-xs text-zinc-500">
                      {row.city || row.place || row.id}
                      {feed ? ` · via ${feed.channel_name || "YouTube"} · ${feed.last_viewers ?? 0} watching` : ""}
                      {!feed && row.live_url ? ` · ${row.live_url}` : ""}
                    </p>
                  </div>
                  <div className="flex flex-wrap justify-end gap-1">
                    {feed ? <Badge tone="green">Checked</Badge> : <Badge tone="amber">Manual</Badge>}
                    {feed?.official ? <Badge tone="blue">Official</Badge> : null}
                    {row.published ? null : <Badge tone="amber">Draft</Badge>}
                    {noUrl ? <Badge tone="red">No URL</Badge> : null}
                    {noPhoto ? <Badge tone="amber">No photo</Badge> : null}
                  </div>
                </Link>
              </li>
            );
          })}
          {liveRows.length === 0 ? (
            <li className="px-4 py-10 text-center text-sm text-zinc-500">No mandirs are live right now.</li>
          ) : null}
        </ul>
      </Card>
      {broken.length > 0 ? (
        <Card className="mt-6 overflow-hidden">
          <div className="border-b border-zinc-200 px-4 py-3">
            <h2 className="text-sm font-semibold">Broken sources</h2>
          </div>
          <ul className="divide-y divide-zinc-100">
            {broken.map((row, index) => {
              const mandir = mandirs.find((item) => item.id === row.mandir_id);
              return (
                <li key={`${row.mandir_id}-${index}`}>
                  <Link
                    href={`/mandirs/${row.mandir_id}`}
                    className="flex items-center justify-between gap-3 px-4 py-3 hover:bg-zinc-50"
                  >
                    <div className="min-w-0">
                      <p className="truncate text-sm font-medium">
                        {mandir?.name ?? row.mandir_id} · {row.channel_name || "YouTube"}
                      </p>
                      <p className="truncate text-xs text-zinc-500">{row.last_error}</p>
                    </div>
                    <Badge tone="red">Fix</Badge>
                  </Link>
                </li>
              );
            })}
          </ul>
        </Card>
      ) : null}
      {photoGaps.length > 0 ? (
        <Card className="mt-6 overflow-hidden">
          <div className="border-b border-zinc-200 px-4 py-3">
            <h2 className="text-sm font-semibold">Missing photos</h2>
          </div>
          <ul className="divide-y divide-zinc-100">
            {photoGaps.map((row) => (
              <li key={row.id}>
                <Link href={`/mandirs/${row.id}`} className="flex items-center justify-between gap-3 px-4 py-3 hover:bg-zinc-50">
                  <div className="min-w-0">
                    <p className="truncate text-sm font-medium">{row.name}</p>
                    <p className="truncate text-xs text-zinc-500">{row.city || row.place || row.id}</p>
                  </div>
                  <Badge tone="amber">No photo</Badge>
                </Link>
              </li>
            ))}
          </ul>
        </Card>
      ) : null}
    </DeskShell>
  );
}
