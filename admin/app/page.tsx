import Link from "next/link";
import { redirect } from "next/navigation";
import { DeskShell, GateMessage } from "@/components/desk-shell";
import { Badge, ButtonLink, Card, PageHeader } from "@/components/ui";
import { requireDesk } from "@/lib/auth";
import type { Mandir } from "@/lib/types";

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

  const [{ data: mandirs }, { count: members }, { count: tiles }] = await Promise.all([
    desk.supabase
      .from("mandirs")
      .select("id,name,live,vr,published,photo_url,city,place")
      .order("sort_order"),
    desk.supabase.from("profiles").select("id", { count: "exact", head: true }),
    desk.supabase.from("home_services").select("id", { count: "exact", head: true }),
  ]);
  const rows = (mandirs ?? []) as Pick<
    Mandir,
    "id" | "name" | "live" | "vr" | "published" | "photo_url" | "city" | "place"
  >[];
  const live = rows.filter((row) => row.live).length;
  const draft = rows.filter((row) => !row.published).length;

  return (
    <DeskShell email={desk.email || desk.staff.email} role={desk.staff.role}>
      <PageHeader
        title="Overview"
        description="Published rows are what the Android app loads on the next launch."
        actions={
          <>
            <ButtonLink href="/mandirs/new">Add mandir</ButtonLink>
            <ButtonLink href="/home" variant="secondary">
              Edit home
            </ButtonLink>
          </>
        }
      />
      <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-5">
        <Stat label="Mandirs" value={rows.length} />
        <Stat label="Live now" value={live} />
        <Stat label="Drafts" value={draft} />
        <Stat label="Home tiles" value={tiles ?? 0} />
        <Stat label="Members" value={members ?? 0} />
      </div>
      <Card className="mt-6 overflow-hidden">
        <div className="flex items-center justify-between border-b border-zinc-200 px-4 py-3">
          <h2 className="text-sm font-semibold">Recent mandirs</h2>
          <ButtonLink href="/mandirs" variant="secondary">
            View all
          </ButtonLink>
        </div>
        <ul className="divide-y divide-zinc-100">
          {rows.slice(0, 8).map((row) => (
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
                  <p className="truncate text-xs text-zinc-500">{row.city || row.place || row.id}</p>
                </div>
                <div className="flex gap-1">
                  <Badge tone={row.published ? "green" : "amber"}>{row.published ? "Published" : "Draft"}</Badge>
                  {row.live ? <Badge tone="red">Live</Badge> : null}
                </div>
              </Link>
            </li>
          ))}
          {rows.length === 0 ? (
            <li className="px-4 py-10 text-center text-sm text-zinc-500">No mandirs yet. Add the first one.</li>
          ) : null}
        </ul>
      </Card>
    </DeskShell>
  );
}

function Stat({ label, value }: { label: string; value: number }) {
  return (
    <Card className="px-4 py-3">
      <p className="text-xs font-medium uppercase tracking-wide text-zinc-500">{label}</p>
      <p className="mt-1 text-2xl font-semibold tabular-nums">{value}</p>
    </Card>
  );
}
