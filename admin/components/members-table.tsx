"use client";

import { useRouter } from "next/navigation";
import { useState } from "react";
import { Alert, Badge, Button, Card, fieldClass } from "@/components/ui";
import { createClient } from "@/lib/supabase/client";
import type { MemberRow } from "@/lib/types";

const tiers = ["None", "Darshan", "Gold", "Platinum", "Nri"];

export function MembersTable({ initial }: { initial: MemberRow[] }) {
  const router = useRouter();
  const [rows, setRows] = useState(initial);
  const [query, setQuery] = useState("");
  const [message, setMessage] = useState("");
  const [ok, setOk] = useState(false);
  const visible = rows.filter((row) => {
    const hay = `${row.display_name} ${row.city} ${row.member_id} ${row.tier}`.toLowerCase();
    const q = query.trim().toLowerCase();
    return !q || hay.includes(q);
  });

  async function save(row: MemberRow) {
    setMessage("");
    setOk(false);
    const supabase = createClient();
    const { error } = await supabase
      .from("profiles")
      .update({
        tier: row.tier,
        member_id: row.member_id,
        valid_until: row.valid_until,
      })
      .eq("id", row.id);
    if (error) {
      setMessage(error.message);
      return;
    }
    setOk(true);
    setMessage("Member updated.");
    router.refresh();
  }

  if (rows.length === 0) {
    return (
      <Card className="p-8 text-center text-sm text-zinc-500">
        No members yet. They appear after someone completes profile setup on the phone.
      </Card>
    );
  }

  return (
    <div className="space-y-3">
      <input
        value={query}
        onChange={(event) => setQuery(event.target.value)}
        placeholder="Search name, city, or member ID"
        className={`${fieldClass} max-w-xs`}
      />
      {message ? <Alert tone={ok ? "ok" : "error"}>{message}</Alert> : null}
      <Card className="overflow-x-auto">
        <table className="w-full min-w-[800px] text-left text-sm">
          <thead className="border-b border-zinc-200 bg-zinc-50 text-xs font-medium uppercase tracking-wide text-zinc-500">
            <tr>
              <th className="px-4 py-3">Member</th>
              <th className="px-4 py-3">Tier</th>
              <th className="px-4 py-3">Member ID</th>
              <th className="px-4 py-3">Valid until</th>
              <th className="px-4 py-3" />
            </tr>
          </thead>
          <tbody>
            {visible.length === 0 ? (
              <tr>
                <td colSpan={5} className="px-4 py-10 text-center text-sm text-zinc-500">
                  No members match.
                </td>
              </tr>
            ) : null}
            {visible.map((row) => (
              <tr key={row.id} className="border-t border-zinc-100">
                <td className="px-4 py-3">
                  <p className="font-medium">{row.display_name || "Unnamed"}</p>
                  <p className="text-xs text-zinc-500">{row.city || "No city"}</p>
                </td>
                <td className="px-4 py-3">
                  <select
                    value={row.tier}
                    onChange={(event) =>
                      setRows((current) =>
                        current.map((item) => (item.id === row.id ? { ...item, tier: event.target.value } : item)),
                      )
                    }
                    className={fieldClass}
                  >
                    {tiers.map((tier) => (
                      <option key={tier}>{tier}</option>
                    ))}
                  </select>
                </td>
                <td className="px-4 py-3">
                  <input
                    value={row.member_id}
                    onChange={(event) =>
                      setRows((current) =>
                        current.map((item) => (item.id === row.id ? { ...item, member_id: event.target.value } : item)),
                      )
                    }
                    className={fieldClass}
                  />
                </td>
                <td className="px-4 py-3">
                  <input
                    value={row.valid_until}
                    onChange={(event) =>
                      setRows((current) =>
                        current.map((item) => (item.id === row.id ? { ...item, valid_until: event.target.value } : item)),
                      )
                    }
                    className={fieldClass}
                  />
                </td>
                <td className="px-4 py-3 text-right">
                  <div className="flex items-center justify-end gap-2">
                    <Badge tone={row.tier === "None" ? "zinc" : "green"}>{row.tier}</Badge>
                    <Button type="button" variant="secondary" onClick={() => save(row)}>
                      Save
                    </Button>
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </Card>
    </div>
  );
}
