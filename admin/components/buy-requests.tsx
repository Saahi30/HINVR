"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { Alert, Badge, Button, Card, fieldClass } from "@/components/ui";
import { createClient } from "@/lib/supabase/client";
import { formatWhen } from "@/lib/ops";
import { formatInr, membershipWindow, nextSerial, tierLabel } from "@/lib/membership";
import type { BuyRequestRow } from "@/lib/types";

type Draft = {
  memberId: string;
  invoice: string;
  purchasedOn: string;
  validFrom: string;
  validUntilIso: string;
  validUntilLabel: string;
  note: string;
};

export function BuyRequests({
  initial,
  memberIds,
  invoiceNumbers,
}: {
  initial: BuyRequestRow[];
  memberIds: string[];
  invoiceNumbers: string[];
}) {
  const router = useRouter();
  const [rows, setRows] = useState(initial);
  const [usedMembers, setUsedMembers] = useState(memberIds);
  const [usedInvoices, setUsedInvoices] = useState(invoiceNumbers);
  const [openId, setOpenId] = useState<string | null>(null);
  const [draft, setDraft] = useState<Draft | null>(null);
  const [notes, setNotes] = useState<Record<string, string>>({});
  const [message, setMessage] = useState("");
  const [ok, setOk] = useState(false);
  const [busyId, setBusyId] = useState<string | null>(null);

  const pending = rows.filter((row) => row.status === "pending");
  const recent = rows.filter((row) => row.status !== "pending").slice(0, 6);

  function openApprove(row: BuyRequestRow) {
    const window = membershipWindow();
    const memberId = row.member_id.trim() || nextSerial(usedMembers, "HNV");
    setOpenId(row.id);
    setDraft({
      memberId,
      invoice: nextSerial(usedInvoices, "INV"),
      note: "",
      ...window,
    });
    setMessage("");
    setOk(false);
  }

  async function approve(row: BuyRequestRow) {
    if (!draft) return;
    const memberId = draft.memberId.trim();
    const invoice = draft.invoice.trim();
    if (!memberId || !invoice || !draft.purchasedOn || !draft.validFrom || !draft.validUntilIso) {
      setMessage("Member ID, invoice, and dates are required.");
      setOk(false);
      return;
    }
    setBusyId(row.id);
    setMessage("");
    setOk(false);
    const supabase = createClient();
    const profile = await supabase
      .from("profiles")
      .update({
        tier: row.tier,
        member_id: memberId,
        valid_until: draft.validUntilLabel,
      })
      .eq("id", row.user_id);
    if (profile.error) {
      setBusyId(null);
      setMessage(profile.error.message);
      return;
    }
    const purchase = await supabase.from("membership_purchases").insert({
      user_id: row.user_id,
      tier: row.tier,
      member_id: memberId,
      amount_inr: row.amount_inr,
      invoice_number: invoice,
      purchased_on: draft.purchasedOn,
      valid_from: draft.validFrom,
      valid_until: draft.validUntilIso,
    });
    if (purchase.error && !/duplicate|unique/i.test(purchase.error.message)) {
      setBusyId(null);
      setMessage(`The plan is on the account. The invoice did not save: ${purchase.error.message}`);
      return;
    }
    const request = await supabase
      .from("membership_requests")
      .update({ status: "approved", staff_note: draft.note.trim() })
      .eq("id", row.id);
    setBusyId(null);
    if (request.error) {
      setMessage(request.error.message);
      return;
    }
    setUsedMembers((current) => (current.includes(memberId) ? current : [...current, memberId]));
    setUsedInvoices((current) => (current.includes(invoice) ? current : [...current, invoice]));
    setRows((current) =>
      current.map((item) =>
        item.id === row.id
          ? { ...item, status: "approved", staff_note: draft.note.trim(), member_id: memberId, current_tier: row.tier }
          : item,
      ),
    );
    setOpenId(null);
    setDraft(null);
    setOk(true);
    setMessage(`${row.display_name || "Member"} is now ${tierLabel(row.tier)}. The pass updates when they reopen the app.`);
    router.refresh();
  }

  async function decline(row: BuyRequestRow) {
    setBusyId(row.id);
    setMessage("");
    setOk(false);
    const note = (notes[row.id] ?? "").trim();
    const supabase = createClient();
    const { error } = await supabase
      .from("membership_requests")
      .update({ status: "declined", staff_note: note })
      .eq("id", row.id);
    setBusyId(null);
    if (error) {
      setMessage(error.message);
      return;
    }
    setRows((current) =>
      current.map((item) => (item.id === row.id ? { ...item, status: "declined", staff_note: note } : item)),
    );
    if (openId === row.id) {
      setOpenId(null);
      setDraft(null);
    }
    setOk(true);
    setMessage("Request declined.");
    router.refresh();
  }

  return (
    <div className="space-y-3">
      {message ? <Alert tone={ok ? "ok" : "err"}>{message}</Alert> : null}
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
              {row.member_id ? ` · ${row.member_id}` : ""}
            </p>
            {openId === row.id && draft ? (
              <div className="mt-4 grid gap-3 sm:grid-cols-2">
                <label className="block text-sm">
                  <span className="mb-1.5 block font-medium text-zinc-700">Member ID</span>
                  <input
                    value={draft.memberId}
                    onChange={(event) => setDraft({ ...draft, memberId: event.target.value })}
                    className={fieldClass}
                  />
                </label>
                <label className="block text-sm">
                  <span className="mb-1.5 block font-medium text-zinc-700">Invoice</span>
                  <input
                    value={draft.invoice}
                    onChange={(event) => setDraft({ ...draft, invoice: event.target.value })}
                    className={fieldClass}
                  />
                </label>
                <label className="block text-sm">
                  <span className="mb-1.5 block font-medium text-zinc-700">Valid from</span>
                  <input
                    type="date"
                    value={draft.validFrom}
                    onChange={(event) => setDraft({ ...draft, purchasedOn: event.target.value, validFrom: event.target.value })}
                    className={fieldClass}
                  />
                </label>
                <label className="block text-sm">
                  <span className="mb-1.5 block font-medium text-zinc-700">Valid until</span>
                  <input
                    type="date"
                    value={draft.validUntilIso}
                    onChange={(event) =>
                      setDraft({
                        ...draft,
                        validUntilIso: event.target.value,
                        validUntilLabel: event.target.value,
                      })
                    }
                    className={fieldClass}
                  />
                </label>
                <label className="block text-sm sm:col-span-2">
                  <span className="mb-1.5 block font-medium text-zinc-700">Note on the pass</span>
                  <input
                    value={draft.note}
                    onChange={(event) => setDraft({ ...draft, note: event.target.value })}
                    placeholder="Optional"
                    className={fieldClass}
                  />
                </label>
                <p className="text-xs text-zinc-500 sm:col-span-2">
                  The phone shows “{draft.validUntilLabel}” as the validity. This does not charge a card.
                </p>
              </div>
            ) : (
              <label className="mt-3 block text-sm">
                <span className="mb-1.5 block font-medium text-zinc-700">Decline note</span>
                <input
                  value={notes[row.id] ?? ""}
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
                    {busyId === row.id ? "Saving…" : "Confirm plan"}
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
