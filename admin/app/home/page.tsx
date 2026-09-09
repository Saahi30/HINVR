import { redirect } from "next/navigation";
import { DeskShell, GateMessage } from "@/components/desk-shell";
import { HomeForm } from "@/components/home-form";
import { PageHeader } from "@/components/ui";
import { requireDesk } from "@/lib/auth";
import type { HomeService, HomeSettings } from "@/lib/types";

export default async function HomeEditorPage() {
  const desk = await requireDesk();
  if (desk.needsSetup) redirect("/setup");
  if (!desk.staff) {
    return <GateMessage title="No access." body="This admin already has an owner." />;
  }

  const { data: services } = await desk.supabase
    .from("home_services")
    .select("*")
    .order("sort_order");
  const { data: setting } = await desk.supabase
    .from("app_settings")
    .select("value")
    .eq("key", "home")
    .maybeSingle();
  const value = (setting?.value ?? {}) as Partial<HomeSettings>;

  return (
    <DeskShell email={desk.email || desk.staff.email} role={desk.staff.role}>
      <PageHeader
        title="Home screen"
        description="Headline and tiles. Add, reorder, or remove — the app grid follows this list."
      />
      <HomeForm
        initialServices={(services ?? []) as HomeService[]}
        initialHome={{
          headline: value.headline ?? "You see the aarti.\nThe internet sees a thumbnail.",
          eyebrow: value.eyebrow ?? "HINVR",
        }}
      />
    </DeskShell>
  );
}
