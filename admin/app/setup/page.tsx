import { redirect } from "next/navigation";
import { ClaimOwner } from "@/components/claim-owner";
import { GateMessage } from "@/components/desk-shell";
import { requireDesk } from "@/lib/auth";

export default async function SetupPage() {
  const desk = await requireDesk();
  if (desk.staff) redirect("/");

  if (desk.needsSetup) {
    return (
      <GateMessage
        title="The database isn't ready."
        body="The desk tables are missing on this project. The owner applies the Supabase migrations (see admin/README.md), then this page works."
      />
    );
  }

  return (
    <GateMessage
      title="Claim this admin"
      body="Only for the first owner. It works once, when the team is empty, and only for an email the project owner has put on the bootstrap list. Everyone else: ask an owner to add you."
      action={<ClaimOwner />}
    />
  );
}
