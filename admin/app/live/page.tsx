import Link from "next/link";
import { redirect } from "next/navigation";
import { DeskShell, GateMessage } from "@/components/desk-shell";
import { Badge, ButtonLink, Card, PageHeader } from "@/components/ui";
import { requireDesk } from "@/lib/auth";
import { catalogHealth } from "@/lib/ops";
import type { Mandir } from "@/lib/types";

export default async function LivePage() {
  const desk = await requireDesk();
  if (desk.needsSetup) redirect("/setup");
  if (!desk.staff) {
    return <GateMessage title="No access." body="This admin already has an owner." />;
  }

  const { data } = await desk.supabase
    .from("mandirs")
    .select("id,name,place,city,photo_url,live,published,live_url,updated_label,updated_at")
    .order("sort_order");

  const mandirs = (data ?? []) as Pick<
    Mandir,
    "id" | "name" | "place" | "city" | "photo_url" | "live" | "published" | "live_url" | "updated_label" | "updated_at"
  >[];
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
        description="Catalog honesty, not a YouTube ping. Flagged live needs a URL, a photo, and a published row."
        actions={<ButtonLink href="/mandirs">Edit mandirs</ButtonLink>}
      />
      <div className="mb-6 grid gap-3 sm:grid-cols-3">
        <Card className="px-4 py-3">
          <p className="text-xs font-medium uppercase tracking-wide text-zinc-500">Live flagged</p>
          <p className="mt-1 text-2xl font-semibold tabular-nums">{health.live.length}</p>
        </Card>
        <Card className="px-4 py-3">
          <p className="text-xs font-medium uppercase tracking-wide text-zinc-500">Missing URL</p>
          <p className="mt-1 text-2xl font-semibold tabular-nums">{health.liveNoUrl.length}</p>
        </Card>
        <Card className="px-4 py-3">
          <p className="text-xs font-medium uppercase tracking-wide text-zinc-500">Live drafts</p>
          <p className="mt-1 text-2xl font-semibold tabular-nums">{health.liveDraft.length}</p>
        </Card>
      </div>
      <Card className="overflow-hidden">
        <div className="border-b border-zinc-200 px-4 py-3">
          <h2 className="text-sm font-semibold">Flagged live</h2>
        </div>
        <ul className="divide-y divide-zinc-100">
          {liveRows.map((row) => {
            const noUrl = !row.live_url?.trim();
            const noPhoto = !row.photo_url?.trim();
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
                      {row.live_url ? ` · ${row.live_url}` : ""}
                    </p>
                  </div>
                  <div className="flex flex-wrap justify-end gap-1">
                    <Badge tone="red">Live</Badge>
                    {row.published ? <Badge tone="green">Published</Badge> : <Badge tone="amber">Draft</Badge>}
                    {noUrl ? <Badge tone="red">No URL</Badge> : null}
                    {noPhoto ? <Badge tone="amber">No photo</Badge> : null}
                  </div>
                </Link>
              </li>
            );
          })}
          {liveRows.length === 0 ? (
            <li className="px-4 py-10 text-center text-sm text-zinc-500">No mandirs are flagged live.</li>
          ) : null}
        </ul>
      </Card>
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
