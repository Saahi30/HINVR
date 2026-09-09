import { redirect } from "next/navigation";
import { DeskShell, GateMessage } from "@/components/desk-shell";
import { MandirForm } from "@/components/mandir-form";
import { ButtonLink, PageHeader } from "@/components/ui";
import { requireDesk } from "@/lib/auth";
import { emptyMandir } from "@/lib/types";

export default async function NewMandirPage() {
  const desk = await requireDesk();
  if (desk.needsSetup) redirect("/setup");
  if (!desk.staff) {
    return <GateMessage title="No access." body="This admin already has an owner." />;
  }

  const { data } = await desk.supabase
    .from("mandirs")
    .select("sort_order")
    .order("sort_order", { ascending: false })
    .limit(1);
  const nextSort = ((data?.[0] as { sort_order?: number } | undefined)?.sort_order ?? 0) + 10;

  return (
    <DeskShell email={desk.email || desk.staff.email} role={desk.staff.role}>
      <PageHeader
        title="Add mandir"
        description="Creates a catalog row the Android app can load after you publish it."
        actions={<ButtonLink href="/mandirs" variant="secondary">Back to list</ButtonLink>}
      />
      <MandirForm initial={emptyMandir(nextSort)} isNew />
    </DeskShell>
  );
}
