import { redirect } from "next/navigation";
import { SendNotice } from "@/components/send-notice";
import { DeskShell, GateMessage } from "@/components/desk-shell";
import { Card, PageHeader } from "@/components/ui";
import { isMissingRelation, requireDesk } from "@/lib/auth";
import type { NoticeRow } from "@/lib/types";

export default async function NotificationsPage() {
  const desk = await requireDesk();
  if (desk.needsSetup) redirect("/setup");
  if (!desk.staff) {
    return <GateMessage title="No access." body="This admin already has an owner." />;
  }

  const { data, error } = await desk.supabase
    .from("notifications")
    .select("id,title,body,sent_count,created_at")
    .order("created_at", { ascending: false })
    .limit(40);

  if (isMissingRelation(error)) {
    return (
      <DeskShell email={desk.email || desk.staff.email} role={desk.staff.role}>
        <PageHeader title="Notifications" description="A notice from the desk, on every signed-in phone." />
        <p className="rounded-xl border border-zinc-200 bg-white px-4 py-8 text-center text-sm text-zinc-500">
          Apply <code className="rounded bg-zinc-100 px-1">supabase/migrations/20260929120000_notifications.sql</code> in
          the HINVR SQL editor, then refresh.
        </p>
      </DeskShell>
    );
  }

  const notices = (data ?? []) as NoticeRow[];

  return (
    <DeskShell email={desk.email || desk.staff.email} role={desk.staff.role}>
      <PageHeader title="Notifications" description="A notice from the desk, on every signed-in phone." />
      <Card className="mb-6 p-4">
        <SendNotice />
      </Card>
      <Card className="overflow-hidden">
        <div className="border-b border-zinc-200 px-4 py-3">
          <h2 className="text-sm font-semibold">Sent</h2>
        </div>
        {notices.length === 0 ? (
          <p className="px-4 py-8 text-center text-sm text-zinc-500">Nothing sent yet.</p>
        ) : (
          <ul className="divide-y divide-zinc-100">
            {notices.map((notice) => (
              <li key={notice.id} className="px-4 py-3">
                <p className="text-sm font-medium">{notice.title}</p>
                <p className="mt-1 text-sm text-zinc-600">{notice.body}</p>
                <p className="mt-1 text-xs text-zinc-500">
                  {new Date(notice.created_at).toLocaleString("en-IN", { dateStyle: "medium", timeStyle: "short" })}
                  {" · "}
                  {notice.sent_count} {notice.sent_count === 1 ? "phone" : "phones"}
                </p>
              </li>
            ))}
          </ul>
        )}
      </Card>
    </DeskShell>
  );
}
