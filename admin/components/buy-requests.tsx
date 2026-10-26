"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { Alert, Badge, Button, Card, fieldClass } from "@/components/ui";
import { createClient } from "@/lib/supabase/client";
import { formatWhen } from "@/lib/ops";
import { formatDate, formatInr, oneYearFrom, tierLabel, todayIst } from "@/lib/membership";
import type { ApprovedMembership, BuyRequestRow } from "@/lib/types";

type Draft = { validFrom: string; note: string };

/**
 * Approve and decline go through approve_membership_request / decline_membership_request:
 * one transaction that locks the request, issues the member ID and invoice number from
 * server sequences, and prices the plan from membership_prices.
 */
export function BuyRequests({ initial }: { initial: BuyRequestRow[] }) {
  const router = useRouter();
  const [rows, setRows] = useState(initial);
  const [openId, setOpenId] = useState<string | null>(null);
  const [draft, setDraft] = useState<Draft>({ validFrom: todayIst(), note: "" });
  const [notes, setNotes] = useState<Record<string, string>>({});
  const [message, setMessage] = useState("");
  const [ok, setOk] = useState(false);
  const [busyId, setBusyId] = useState<string | null>(null);

  const pending = rows.filter((row) => row.status === "pending");
  const recent = rows.filter((row) => row.status !== "pending").slice(0, 6);

  function openApprove(row: BuyRequestRow) {
    setOpenId(row.id);
    setDraft({ validFrom: todayIst(), note: "" });
    setMessage("");
    setOk(false);
  }

  async function approve(row: BuyRequestRow) {
    if (!draft.validFrom) {
      setMessage("Pick the date the plan starts.");
      setOk(false);
      return;
    }
    setBusyId(row.id);
    setMessage("");
    setOk(false);
    const { data, error } = await createClient().rpc("approve_membership_request", {
      p_request_id: row.id,
      p_valid_from: draft.validFrom,
      p_note: draft.note.trim(),
    });
    setBusyId(null);
    if (error || !data) {
      setMessage(error?.message ?? "Couldn't approve this request.");
      router.refresh();
      return;
    }
    const result = data as ApprovedMembership;
    setRows((current) =>
      current.map((item) =>
        item.id === row.id
          ? { ...item, status: "approved", staff_note: draft.note.trim(), member_id: result.member_id, current_tier: row.tier }
          : item,
      ),
    );
    setOpenId(null);
    setOk(true);
    setMessage(
      `${row.display_name || "Member"} is now ${tierLabel(result.tier)} (${result.member_id}) until ${formatDate(result.valid_until)}. Invoice ${result.invoice_number}. The pass updates when they reopen the app.`,
    );
    router.refresh();
  }

  async function decline(row: BuyRequestRow) {
    const note = (notes[row.id] ?? "").trim();
    if (!confirm(`Decline ${row.display_name || "this member"}'s ${tierLabel(row.tier)} request?`)) return;
    setBusyId(row.id);
    setMessage("");
    setOk(false);
    const { error } = await createClient().rpc("decline_membership_request", { p_request_id: row.id, p_reason: note });
    setBusyId(null);
    if (error) {
      setMessage(error.message);
      router.refresh();
      return;
    }
    setRows((current) =>
      current.map((item) => (item.id === row.id ? { ...item, status: "declined", staff_note: note } : item)),
    );
    if (openId === row.id) setOpenId(null);
    setOk(true);
    setMessage("Request declined.");
    router.refresh();
  }

  return (
    <div className="space-y-3">
      {message ? <Alert tone={ok ? "ok" : "error"}>{message}</Alert> : null}
      {pending.length === 0 ? (
        <Card className="p-8 text-center text-sm text-zinc-500">No buy requests waiting.</Card>
      ) : (
        pending.map((row) => (
          <Card key={row.id} className="p-4">
            <div className="flex flex-wrap items-start justify-between gap-3">
              <div>
                <p className="text-sm font-semibold text-zinc-900">{row.display_name || "Unnamed"}</p>
                <p className="mt-1 text-sm text-zinc-500">
                  {[row.phone_e164, row.city, formatWhen(row.created_at)].filter(Boolean).join(" · ")}
                </p>
              </div>
              <div className="flex items-center gap-2">
                <Badge tone="amber">{tierLabel(row.tier)}</Badge>
                <span className="text-sm font-medium tabular-nums">{formatInr(row.amount_inr)}</span>
              </div>
            </div>
            <p className="mt-2 text-xs text-zinc-500">
              On the account now: {tierLabel(row.current_tier)}
              {row.member_id ? ` · ${row.member_id}` : ""} · price set by the server from the plan table
            </p>
            {openId === row.id ? (
              <div className="mt-4 grid gap-3 sm:grid-cols-2">
                <label className="block text-sm">
                  <span className="mb-1.5 block font-medium text-zinc-700">Valid from</span>
                  <input
                    type="date"
                    value={draft.validFrom}
                    onChange={(event) => setDraft({ ...draft, validFrom: event.target.value })}
                    className={fieldClass}
                  />
                </label>
                <div className="text-sm">
                  <span className="mb-1.5 block font-medium text-zinc-700">Valid until</span>
                  <p className="py-2 text-zinc-700">{formatDate(oneYearFrom(draft.validFrom))} · 23:59 IST</p>
                </div>
                <label className="block text-sm sm:col-span-2">
                  <span className="mb-1.5 block font-medium text-zinc-700">Note to the member</span>
                  <input
                    value={draft.note}
                    maxLength={1000}
                    onChange={(event) => setDraft({ ...draft, note: event.target.value })}
                    placeholder="Optional"
                    className={fieldClass}
                  />
                </label>
                <p className="text-xs text-zinc-500 sm:col-span-2">
                  {row.member_id ? `Keeps member ID ${row.member_id}.` : "Issues the next member ID."} Issues the next
                  invoice number. This does not charge a card.
                </p>
              </div>
            ) : (
              <label className="mt-3 block text-sm">
                <span className="mb-1.5 block font-medium text-zinc-700">Decline note</span>
                <input
                  value={notes[row.id] ?? ""}
                  maxLength={1000}
                  onChange={(event) => setNotes((current) => ({ ...current, [row.id]: event.target.value }))}
                  placeholder="Optional, shown in the app"
                  className={fieldClass}
                />
              </label>
            )}
            <div className="mt-4 flex flex-wrap gap-2">
              {openId === row.id ? (
                <>
                  <Button type="button" disabled={busyId === row.id} onClick={() => approve(row)}>
                    {busyId === row.id ? "Saving…" : `Confirm ${tierLabel(row.tier)}`}
                  </Button>
                  <Button type="button" variant="secondary" onClick={() => setOpenId(null)}>
                    Back
                  </Button>
                </>
              ) : (
                <>
                  <Button type="button" disabled={busyId === row.id} onClick={() => openApprove(row)}>
                    Approve
                  </Button>
                  <Button type="button" variant="danger" disabled={busyId === row.id} onClick={() => decline(row)}>
                    Decline
                  </Button>
                </>
              )}
            </div>
          </Card>
        ))
      )}
      {recent.length > 0 ? (
        <div className="pt-2">
          <p className="mb-2 text-xs font-medium tracking-wide text-zinc-500 uppercase">Recent</p>
          <ul className="divide-y divide-zinc-100 rounded-xl border border-zinc-200 bg-white">
            {recent.map((row) => (
              <li key={row.id} className="flex flex-wrap items-center justify-between gap-2 px-4 py-3 text-sm">
                <span>
                  {row.display_name || "Unnamed"} · {tierLabel(row.tier)} · {formatInr(row.amount_inr)}
                </span>
                <Badge tone={row.status === "approved" ? "green" : "zinc"}>{row.status}</Badge>
              </li>
            ))}
          </ul>
        </div>
      ) : null}
    </div>
  );
}
