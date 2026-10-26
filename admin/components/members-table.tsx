"use client";

import { useRouter } from "next/navigation";
import { useState } from "react";
import { Dialog } from "@/components/dialog";
import { Alert, Badge, Button, Card, FilterChip, Field, fieldClass } from "@/components/ui";
import { formatDate, formatInr, tierLabel } from "@/lib/membership";
import { createClient } from "@/lib/supabase/client";
import { TIERS, type MemberRow } from "@/lib/types";

export type MemberFilters = { q: string; tier: string; funnel: "all" | "complete" | "incomplete" };

type Draft = {
  tier: string;
  member_id: string;
  valid_until: string;
  invoice_number: string;
  amount_inr: string;
  purchased_on: string;
  valid_from: string;
  invoice_valid_until: string;
};

function draftOf(row: MemberRow): Draft {
  return {
    tier: row.tier,
    member_id: row.member_id,
    valid_until: (row.valid_until ?? "").slice(0, 10),
    invoice_number: row.invoice_number ?? "",
    amount_inr: row.amount_inr == null ? "" : String(row.amount_inr),
    purchased_on: (row.purchased_on ?? "").slice(0, 10),
    valid_from: (row.valid_from ?? "").slice(0, 10),
    invoice_valid_until: (row.invoice_valid_until ?? "").slice(0, 10),
  };
}

export function MembersTable({
  rows,
  total,
  page,
  pageSize,
  filters,
  canEdit,
}: {
  rows: MemberRow[];
  total: number;
  page: number;
  pageSize: number;
  filters: MemberFilters;
  canEdit: boolean;
}) {
  const router = useRouter();
  const [query, setQuery] = useState(filters.q);
  const [editing, setEditing] = useState<MemberRow | null>(null);
  const [message, setMessage] = useState("");
  const [ok, setOk] = useState(false);
  const pages = Math.max(1, Math.ceil(total / pageSize));

  function go(change: Record<string, string>) {
    const next = new URLSearchParams();
    if (filters.q) next.set("q", filters.q);
    if (filters.tier !== "all") next.set("tier", filters.tier);
    if (filters.funnel !== "all") next.set("funnel", filters.funnel);
    if (page > 1) next.set("page", String(page));
    for (const [key, value] of Object.entries(change)) {
      if (!value || value === "all" || (key === "page" && value === "1")) next.delete(key);
      else next.set(key, value);
    }
    if (!("page" in change)) next.delete("page");
    const search = next.toString();
    router.push(search ? `/members?${search}` : "/members");
  }

  return (
    <div className="space-y-3">
      <form
        className="flex flex-wrap gap-2"
        role="search"
        onSubmit={(event) => {
          event.preventDefault();
          go({ q: query.trim() });
        }}
      >
        <input
          value={query}
          onChange={(event) => setQuery(event.target.value)}
          placeholder={canEdit ? "Name, city, phone, member ID, or invoice" : "Name, city, phone, or member ID"}
          aria-label="Search members"
          className={`${fieldClass} max-w-xs`}
        />
        <Button type="submit" variant="secondary">
          Search
        </Button>
      </form>
      <div className="flex flex-wrap gap-2" aria-label="Profile filter">
        {(["all", "complete", "incomplete"] as const).map((item) => (
          <FilterChip key={item} active={filters.funnel === item} onClick={() => go({ funnel: item })}>
            {item === "all" ? "All" : item === "complete" ? "Complete" : "Incomplete"}
          </FilterChip>
        ))}
        {(["all", ...TIERS] as const).map((item) => (
          <FilterChip key={item} active={filters.tier === item} onClick={() => go({ tier: item })}>
            {item === "all" ? "All tiers" : tierLabel(item)}
          </FilterChip>
        ))}
      </div>
      {message ? <Alert tone={ok ? "ok" : "error"}>{message}</Alert> : null}
      <Card className="overflow-x-auto">
        <table className="w-full min-w-[820px] text-left text-sm">
          <thead className="border-b border-zinc-200 bg-zinc-50 text-xs font-medium uppercase tracking-wide text-zinc-500">
            <tr>
              <th className="px-4 py-3">Member</th>
              <th className="px-4 py-3">Plan</th>
              <th className="px-4 py-3">Member ID</th>
              <th className="px-4 py-3">Valid until</th>
              {canEdit ? <th className="px-4 py-3">Latest invoice</th> : null}
              <th className="px-4 py-3">Profile</th>
              {canEdit ? <th className="px-4 py-3" /> : null}
            </tr>
          </thead>
          <tbody>
            {rows.length === 0 ? (
              <tr>
                <td colSpan={canEdit ? 7 : 5} className="px-4 py-10 text-center text-sm text-zinc-500">
                  {total === 0 && !filters.q && filters.tier === "all" && filters.funnel === "all"
                    ? "No members yet. They appear after someone completes profile setup on the phone."
                    : "No members match."}
                </td>
              </tr>
            ) : null}
            {rows.map((row) => (
              <tr key={row.id} className="border-t border-zinc-100 align-top">
                <td className="px-4 py-3">
                  <p className="font-medium">{row.display_name || "Unnamed"}</p>
                  <p className="text-xs text-zinc-500">{row.phone_e164 || "No phone yet"}</p>
                  <p className="text-xs text-zinc-400">{row.city || "No city"}</p>
                  {(row.addresses ?? []).map((place) => (
                    <p key={`${place.label}-${place.address}`} className="text-xs text-zinc-400">
                      {place.label}: {place.address}
                    </p>
                  ))}
                </td>
                <td className="px-4 py-3">
                  <Badge tone={row.tier === "None" ? "zinc" : "green"}>{tierLabel(row.tier)}</Badge>
                </td>
                <td className="px-4 py-3 tabular-nums">{row.member_id || "—"}</td>
                <td className="px-4 py-3">{formatDate(row.valid_until)}</td>
                {canEdit ? (
                  <td className="px-4 py-3">
                    {row.invoice_number ? (
                      <>
                        <p className="tabular-nums">{row.invoice_number}</p>
                        <p className="text-xs text-zinc-500">
                          {row.amount_inr != null ? formatInr(row.amount_inr) : ""} · {formatDate(row.purchased_on)}
                        </p>
                      </>
                    ) : (
                      "—"
                    )}
                  </td>
                ) : null}
                <td className="px-4 py-3">
                  <Badge tone={row.profile_complete ? "green" : "amber"}>
                    {row.profile_complete ? "Complete" : "Incomplete"}
                  </Badge>
                </td>
                {canEdit ? (
                  <td className="px-4 py-3 text-right">
                    <Button
                      type="button"
                      variant="secondary"
                      onClick={() => {
                        setMessage("");
                        setEditing(row);
                      }}
                    >
                      Edit
                    </Button>
                  </td>
                ) : null}
              </tr>
            ))}
          </tbody>
        </table>
      </Card>
      <nav className="flex flex-wrap items-center justify-between gap-3 text-sm text-zinc-500" aria-label="Pages">
        <p>
          {total === 0 ? "0 members" : `${(page - 1) * pageSize + 1}–${Math.min(page * pageSize, total)} of ${total}`}
        </p>
        <div className="flex gap-2">
          <Button type="button" variant="secondary" disabled={page <= 1} onClick={() => go({ page: String(page - 1) })}>
            Previous
          </Button>
          <Button type="button" variant="secondary" disabled={page >= pages} onClick={() => go({ page: String(page + 1) })}>
            Next
          </Button>
        </div>
      </nav>
      {editing ? (
        <EditMember
          key={editing.id}
          row={editing}
          onClose={() => setEditing(null)}
          onSaved={(text) => {
            setEditing(null);
            setOk(true);
            setMessage(text);
            router.refresh();
          }}
        />
      ) : null}
    </div>
  );
}

function EditMember({ row, onClose, onSaved }: { row: MemberRow; onClose: () => void; onSaved: (message: string) => void }) {
  const initial = draftOf(row);
  const [draft, setDraft] = useState(initial);
  const [confirming, setConfirming] = useState(false);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");

  const membershipChanged =
    draft.tier !== initial.tier || draft.valid_until !== initial.valid_until || draft.member_id.trim() !== initial.member_id;
  const invoiceChanged =
    draft.invoice_number.trim() !== initial.invoice_number ||
    draft.amount_inr !== initial.amount_inr ||
    draft.purchased_on !== initial.purchased_on ||
    draft.valid_from !== initial.valid_from ||
    draft.invoice_valid_until !== initial.invoice_valid_until;
  const dirty = membershipChanged || invoiceChanged;

  function patch(change: Partial<Draft>) {
    setDraft((current) => ({ ...current, ...change }));
    setConfirming(false);
  }

  async function save() {
    setError("");
    const invoice = draft.invoice_number.trim();
    const amount = Number(draft.amount_inr);
    if (draft.tier !== "None" && (!draft.member_id.trim() || !draft.valid_until)) {
      setError("A plan needs a member ID and a valid-until date.");
      return;
    }
    if (invoiceChanged && invoice) {
      if (!draft.purchased_on || !draft.valid_from || !draft.invoice_valid_until || !Number.isFinite(amount) || amount < 0) {
        setError("An invoice needs a purchase date, valid from, valid until, and an amount.");
        return;
      }
      if (draft.invoice_valid_until < draft.valid_from) {
        setError("Invoice valid until must be on or after valid from.");
        return;
      }
    }
    if (membershipChanged && !confirming) {
      setConfirming(true);
      return;
    }
    setBusy(true);
    const supabase = createClient();
    if (membershipChanged) {
      const { error: profileError } = await supabase
        .from("profiles")
        .update({ tier: draft.tier, member_id: draft.member_id.trim(), valid_until: draft.valid_until || null })
        .eq("id", row.id);
      if (profileError) {
        setBusy(false);
        setError(profileError.message);
        return;
      }
    }
    if (invoiceChanged && invoice) {
      const purchase = {
        user_id: row.id,
        tier: draft.tier === "None" ? row.tier : draft.tier,
        member_id: draft.member_id.trim(),
        amount_inr: amount,
        invoice_number: invoice,
        purchased_on: draft.purchased_on,
        valid_from: draft.valid_from,
        valid_until: draft.invoice_valid_until,
      };
      const saved = row.purchase_id
        ? await supabase.from("membership_purchases").update(purchase).eq("id", row.purchase_id)
        : await supabase.from("membership_purchases").insert(purchase);
      if (saved.error) {
        setBusy(false);
        setError(
          membershipChanged
            ? `The plan was saved, but the invoice was not: ${saved.error.message}`
            : saved.error.message,
        );
        return;
      }
    }
    setBusy(false);
    onSaved(`${row.display_name || "Member"} updated.`);
  }

  return (
    <Dialog open title={`Edit ${row.display_name || "member"}`} onClose={onClose} wide>
      <div className="grid gap-4 sm:grid-cols-3">
        <Field label="Plan">
          <select value={draft.tier} onChange={(event) => patch({ tier: event.target.value })} className={fieldClass}>
            {TIERS.map((item) => (
              <option key={item} value={item}>
                {tierLabel(item)}
              </option>
            ))}
          </select>
        </Field>
        <Field label="Member ID">
          <input value={draft.member_id} onChange={(event) => patch({ member_id: event.target.value })} className={fieldClass} />
        </Field>
        <Field label="Valid until" hint="Ends 23:59 IST that day">
          <input
            type="date"
            value={draft.valid_until}
            onChange={(event) => patch({ valid_until: event.target.value })}
            className={fieldClass}
          />
        </Field>
      </div>
      <p className="mt-5 text-xs font-medium tracking-wide text-zinc-500 uppercase">
        {row.purchase_id ? "Latest invoice" : "Add an invoice (optional)"}
      </p>
      <div className="mt-2 grid gap-4 sm:grid-cols-2">
        <Field label="Invoice number">
          <input
            value={draft.invoice_number}
            placeholder="INV-2026-0001"
            onChange={(event) => patch({ invoice_number: event.target.value })}
            className={fieldClass}
          />
        </Field>
        <Field label="Amount (₹)">
          <input
            type="number"
            min={0}
            value={draft.amount_inr}
            onChange={(event) => patch({ amount_inr: event.target.value })}
            className={fieldClass}
          />
        </Field>
        <Field label="Bought on">
          <input type="date" value={draft.purchased_on} onChange={(event) => patch({ purchased_on: event.target.value })} className={fieldClass} />
        </Field>
        <Field label="Invoice valid from">
          <input type="date" value={draft.valid_from} onChange={(event) => patch({ valid_from: event.target.value })} className={fieldClass} />
        </Field>
        <Field label="Invoice valid until">
          <input
            type="date"
            value={draft.invoice_valid_until}
            onChange={(event) => patch({ invoice_valid_until: event.target.value })}
            className={fieldClass}
          />
        </Field>
      </div>
      {confirming ? (
        <div className="mt-5 rounded-lg border border-amber-200 bg-amber-50 p-3 text-sm text-amber-900" role="alert">
          Change the plan from {tierLabel(initial.tier)} until {formatDate(initial.valid_until)} to {tierLabel(draft.tier)}
          {draft.valid_until ? ` until ${formatDate(draft.valid_until)}` : ""}? Their current pass stops working and the
          phone gets a new one. This is recorded in the audit log.
        </div>
      ) : null}
      {error ? (
        <div className="mt-4">
          <Alert>{error}</Alert>
        </div>
      ) : null}
      <div className="mt-5 flex flex-wrap justify-end gap-2">
        <Button type="button" variant="secondary" onClick={onClose}>
          Cancel
        </Button>
        <Button type="button" disabled={!dirty || busy} onClick={save}>
          {busy ? "Saving…" : confirming ? "Yes, change the plan" : "Save"}
        </Button>
      </div>
    </Dialog>
  );
}
