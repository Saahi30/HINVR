"use client";

import { useMemo, useState } from "react";
import { useRouter } from "next/navigation";
import { Alert, Badge, Button, Card, fieldClass } from "@/components/ui";
import { createClient } from "@/lib/supabase/client";
import { formatWhen } from "@/lib/ops";
import { DESK_KINDS, DESK_STATUSES, type DeskRequestKind, type DeskRequestRow, type DeskRequestStatus } from "@/lib/types";

type KindFilter = "all" | DeskRequestKind;
type StatusFilter = "all" | DeskRequestStatus;

export function RequestsTable({ initial }: { initial: DeskRequestRow[] }) {
  const router = useRouter();
  const [rows, setRows] = useState(initial);
  const [kind, setKind] = useState<KindFilter>("all");
  const [status, setStatus] = useState<StatusFilter>("all");
  const [query, setQuery] = useState("");
  const [message, setMessage] = useState("");
  const [ok, setOk] = useState(false);
  const [busyId, setBusyId] = useState<string | null>(null);

  const visible = useMemo(() => {
    const q = query.trim().toLowerCase();
    return rows.filter((row) => {
      if (kind !== "all" && row.kind !== kind) return false;
      if (status !== "all" && row.status !== status) return false;
      if (!q) return true;
      const hay = `${row.display_name} ${row.city} ${row.summary} ${row.phone_e164} ${row.kind}`.toLowerCase();
      return hay.includes(q);
    });
  }, [rows, kind, status, query]);

  async function save(row: DeskRequestRow) {
    setBusyId(row.id);
    setMessage("");
    setOk(false);
    const supabase = createClient();
    const { error } = await supabase
      .from("desk_requests")
      .update({ status: row.status, staff_note: row.staff_note })
      .eq("id", row.id);
    setBusyId(null);
    if (error) {
      setMessage(error.message);
      return;
    }
    setOk(true);
    setMessage("Request updated.");
    router.refresh();
  }

  if (rows.length === 0) {
    return (
      <Card className="p-8 text-center text-sm text-zinc-500">
        Inbox is empty. Visit, concierge, pooja, and yatra sends from the app show up here.
      </Card>
    );
  }

  return (
    <div className="space-y-3">
      <div className="flex flex-wrap gap-2">
        <input
          value={query}
          onChange={(event) => setQuery(event.target.value)}
          placeholder="Search name, city, or note"
          className={`${fieldClass} max-w-xs`}
        />
        {(["all", ...DESK_KINDS] as const).map((item) => (
          <button
            key={item}
            type="button"
            onClick={() => setKind(item)}
            className={
              kind === item
                ? "rounded-md bg-zinc-900 px-3 py-2 text-sm text-white"
                : "rounded-md border border-zinc-200 bg-white px-3 py-2 text-sm text-zinc-600"
            }
          >
            {item === "all" ? "All kinds" : item}
          </button>
        ))}
      </div>
      <div className="flex flex-wrap gap-2">
        {(["all", ...DESK_STATUSES] as const).map((item) => (
          <button
            key={item}
            type="button"
            onClick={() => setStatus(item)}
            className={
              status === item
                ? "rounded-md bg-zinc-900 px-3 py-2 text-sm text-white"
                : "rounded-md border border-zinc-200 bg-white px-3 py-2 text-sm text-zinc-600"
            }
          >
            {item === "all" ? "All status" : item}
          </button>
        ))}
      </div>
      {message ? <Alert tone={ok ? "ok" : "error"}>{message}</Alert> : null}
      <Card className="overflow-x-auto">
        <table className="w-full min-w-[960px] text-left text-sm">
          <thead className="border-b border-zinc-200 bg-zinc-50 text-xs font-medium uppercase tracking-wide text-zinc-500">
            <tr>
              <th className="px-4 py-3">Member</th>
              <th className="px-4 py-3">Request</th>
              <th className="px-4 py-3">Status</th>
              <th className="px-4 py-3">Desk note</th>
              <th className="px-4 py-3" />
            </tr>
          </thead>
          <tbody>
            {visible.length === 0 ? (
              <tr>
                <td colSpan={5} className="px-4 py-10 text-center text-sm text-zinc-500">
                  No requests match.
                </td>
              </tr>
            ) : null}
            {visible.map((row) => (
              <tr key={row.id} className="border-t border-zinc-100 align-top">
                <td className="px-4 py-3">
                  <p className="font-medium">{row.display_name}</p>
                  <p className="text-xs text-zinc-500">{row.phone_e164 || "No phone yet"}</p>
                  <p className="text-xs text-zinc-400">
                    {row.city || "No city"} · {row.tier}
                  </p>
                </td>
                <td className="px-4 py-3">
                  <div className="flex items-center gap-1.5">
                    <Badge tone={row.kind === "VISIT" ? "blue" : row.kind === "CONCIERGE" ? "green" : "amber"}>
                      {row.kind}
                    </Badge>
                    <span className="text-[11px] text-zinc-400">{formatWhen(row.created_at)}</span>
                  </div>
                  <p className="mt-1 max-w-md text-sm text-zinc-700">{row.summary}</p>
                </td>
                <td className="px-4 py-3">
                  <select
                    value={row.status}
                    onChange={(event) =>
                      setRows((current) =>
                        current.map((item) =>
                          item.id === row.id ? { ...item, status: event.target.value as DeskRequestStatus } : item,
                        ),
                      )
                    }
                    className={fieldClass}
                  >
                    {DESK_STATUSES.map((item) => (
                      <option key={item}>{item}</option>
                    ))}
                  </select>
                </td>
                <td className="px-4 py-3">
                  <input
                    value={row.staff_note}
                    onChange={(event) =>
                      setRows((current) =>
                        current.map((item) => (item.id === row.id ? { ...item, staff_note: event.target.value } : item)),
                      )
                    }
                    placeholder="Note for the desk"
                    className={fieldClass}
                  />
                </td>
                <td className="px-4 py-3 text-right">
                  <Button type="button" variant="secondary" disabled={busyId === row.id} onClick={() => save(row)}>
                    Save
                  </Button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </Card>
    </div>
  );
}
