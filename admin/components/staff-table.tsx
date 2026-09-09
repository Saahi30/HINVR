"use client";

import { useRouter } from "next/navigation";
import { useState } from "react";
import { Alert, Badge, Button, Card, Field, fieldClass } from "@/components/ui";
import { createClient } from "@/lib/supabase/client";
import type { Staff } from "@/lib/types";

export function StaffTable({
  initial,
  me,
  isOwner,
}: {
  initial: Staff[];
  me: string;
  isOwner: boolean;
}) {
  const router = useRouter();
  const [rows, setRows] = useState(initial);
  const [email, setEmail] = useState("");
  const [message, setMessage] = useState("");
  const [ok, setOk] = useState(false);
  const [busy, setBusy] = useState(false);

  async function invite(event: React.FormEvent) {
    event.preventDefault();
    setBusy(true);
    setMessage("");
    setOk(false);
    try {
      const supabase = createClient();
      const { error } = await supabase.rpc("invite_staff", { p_email: email.trim() });
      if (error) throw error;
      setEmail("");
      setOk(true);
      setMessage("Editor added.");
      router.refresh();
    } catch (error) {
      setMessage(error instanceof Error ? error.message : "Could not invite.");
    } finally {
      setBusy(false);
    }
  }

  async function remove(row: Staff) {
    if (!confirm(`Remove ${row.email} from admin?`)) return;
    setMessage("");
    const supabase = createClient();
    const { error } = await supabase.from("staff").delete().eq("user_id", row.user_id);
    if (error) {
      setMessage(error.message);
      return;
    }
    setRows((current) => current.filter((item) => item.user_id !== row.user_id));
    router.refresh();
  }

  return (
    <div className="space-y-4">
      {isOwner ? (
        <Card className="p-5">
          <form onSubmit={invite} className="flex flex-wrap items-end gap-3">
            <div className="min-w-[220px] flex-1">
              <Field label="Invite by email" hint="They must already have created an account on this admin.">
                <input
                  type="email"
                  required
                  value={email}
                  onChange={(event) => setEmail(event.target.value)}
                  placeholder="editor@studio.com"
                  className={fieldClass}
                />
              </Field>
            </div>
            <Button type="submit" disabled={busy}>
              {busy ? "Adding…" : "Add editor"}
            </Button>
          </form>
        </Card>
      ) : null}
      {message ? <Alert tone={ok ? "ok" : "error"}>{message}</Alert> : null}
      <Card className="overflow-x-auto">
        <table className="w-full min-w-[520px] text-left text-sm">
          <thead className="border-b border-zinc-200 bg-zinc-50 text-xs font-medium tracking-wide text-zinc-500 uppercase">
            <tr>
              <th className="px-4 py-3">Email</th>
              <th className="px-4 py-3">Role</th>
              <th className="px-4 py-3 text-right">Actions</th>
            </tr>
          </thead>
          <tbody>
            {rows.map((row) => (
              <tr key={row.user_id} className="border-t border-zinc-100">
                <td className="px-4 py-3">
                  <p className="font-medium">{row.email || row.user_id}</p>
                  {row.user_id === me ? <p className="text-xs text-zinc-500">You</p> : null}
                </td>
                <td className="px-4 py-3">
                  <Badge tone={row.role === "owner" ? "blue" : "zinc"}>{row.role}</Badge>
                </td>
                <td className="px-4 py-3 text-right">
                  {isOwner && row.user_id !== me && row.role !== "owner" ? (
                    <button
                      type="button"
                      onClick={() => remove(row)}
                      className="rounded-md px-2 py-1 text-sm text-red-600 hover:bg-red-50"
                    >
                      Remove
                    </button>
                  ) : (
                    <span className="text-xs text-zinc-400">—</span>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </Card>
    </div>
  );
}
