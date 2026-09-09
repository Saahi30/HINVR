"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useMemo, useState } from "react";
import { Alert, Badge, Button, Card, fieldClass } from "@/components/ui";
import { createClient } from "@/lib/supabase/client";
import type { Mandir } from "@/lib/types";

type Filter = "all" | "published" | "draft" | "live";

export function MandirsTable({ initial }: { initial: Mandir[] }) {
  const router = useRouter();
  const [rows, setRows] = useState(initial);
  const [query, setQuery] = useState("");
  const [filter, setFilter] = useState<Filter>("all");
  const [message, setMessage] = useState("");
  const [busyId, setBusyId] = useState<string | null>(null);

  const visible = useMemo(() => {
    return rows.filter((row) => {
      const hay = `${row.name} ${row.city} ${row.place}`.toLowerCase();
      const q = query.trim().toLowerCase();
      if (q && !hay.includes(q)) return false;
      if (filter === "published") return row.published;
      if (filter === "draft") return !row.published;
      if (filter === "live") return row.live;
      return true;
    });
  }, [rows, query, filter]);

  async function remove(row: Mandir) {
    if (!confirm(`Delete ${row.name}? It will disappear from the app on next launch.`)) return;
    setBusyId(row.id);
    setMessage("");
    const supabase = createClient();
    const { error } = await supabase.from("mandirs").delete().eq("id", row.id);
    setBusyId(null);
    if (error) {
      setMessage(error.message);
      return;
    }
    setRows((current) => current.filter((item) => item.id !== row.id));
    router.refresh();
  }

  async function togglePublished(row: Mandir) {
    setBusyId(row.id);
    const supabase = createClient();
    const { error } = await supabase.from("mandirs").update({ published: !row.published }).eq("id", row.id);
    setBusyId(null);
    if (error) {
      setMessage(error.message);
      return;
    }
    setRows((current) =>
      current.map((item) => (item.id === row.id ? { ...item, published: !item.published } : item)),
    );
    router.refresh();
  }

  return (
    <div className="space-y-3">
      <div className="flex flex-wrap gap-2">
        <input
          value={query}
          onChange={(event) => setQuery(event.target.value)}
          placeholder="Search name or city"
          className={`${fieldClass} max-w-xs`}
        />
        {(["all", "published", "draft", "live"] as const).map((item) => (
          <button
            key={item}
            type="button"
            onClick={() => setFilter(item)}
            className={
              filter === item
                ? "rounded-md bg-zinc-900 px-3 py-2 text-sm text-white"
                : "rounded-md border border-zinc-200 bg-white px-3 py-2 text-sm text-zinc-600"
            }
          >
            {item[0].toUpperCase() + item.slice(1)}
          </button>
        ))}
      </div>
      {message ? <Alert>{message}</Alert> : null}
      <Card className="overflow-x-auto">
        <table className="w-full min-w-[720px] text-left text-sm">
          <thead className="border-b border-zinc-200 bg-zinc-50 text-xs font-medium uppercase tracking-wide text-zinc-500">
            <tr>
              <th className="px-4 py-3">Mandir</th>
              <th className="px-4 py-3">Status</th>
              <th className="px-4 py-3">Media</th>
              <th className="px-4 py-3 text-right">Actions</th>
            </tr>
          </thead>
          <tbody>
            {visible.map((row) => (
              <tr key={row.id} className="border-t border-zinc-100 hover:bg-zinc-50/80">
                <td className="px-4 py-3">
                  <Link href={`/mandirs/${row.id}`} className="font-medium text-zinc-900 hover:underline">
                    {row.name}
                  </Link>
                  <p className="text-xs text-zinc-500">{row.city || row.place || row.id}</p>
                </td>
                <td className="px-4 py-3">
                  <div className="flex flex-wrap gap-1">
                    <Badge tone={row.published ? "green" : "amber"}>{row.published ? "Published" : "Draft"}</Badge>
                    {row.live ? <Badge tone="red">Live</Badge> : null}
                    {row.vr ? <Badge tone="blue">VR</Badge> : null}
                    {row.pass_accepted ? <Badge>Pass</Badge> : null}
                  </div>
                </td>
                <td className="px-4 py-3 text-zinc-500">
                  {[row.photo_url ? "Photo" : null, row.live_url ? "Live URL" : null, row.vr_url ? "VR URL" : null]
                    .filter(Boolean)
                    .join(" · ") || "None"}
                </td>
                <td className="px-4 py-3">
                  <div className="flex justify-end gap-2">
                    <Link href={`/mandirs/${row.id}`} className="rounded-md px-2 py-1 text-sm text-zinc-600 hover:bg-zinc-100">
                      Edit
                    </Link>
                    <button
                      type="button"
                      disabled={busyId === row.id}
                      onClick={() => togglePublished(row)}
                      className="rounded-md px-2 py-1 text-sm text-zinc-600 hover:bg-zinc-100"
                    >
                      {row.published ? "Unpublish" : "Publish"}
                    </button>
                    <button
                      type="button"
                      disabled={busyId === row.id}
                      onClick={() => remove(row)}
                      className="rounded-md px-2 py-1 text-sm text-red-600 hover:bg-red-50"
                    >
                      Delete
                    </button>
                  </div>
                </td>
              </tr>
            ))}
            {visible.length === 0 ? (
              <tr>
                <td colSpan={4} className="px-4 py-10 text-center text-sm text-zinc-500">
                  No mandirs match. Add one, or clear filters.
                </td>
              </tr>
            ) : null}
          </tbody>
        </table>
      </Card>
    </div>
  );
}
