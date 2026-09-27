"use client";

import { useMemo, useState } from "react";
import { useRouter } from "next/navigation";
import { Alert, Badge, Button, Card, fieldClass } from "@/components/ui";
import { createClient } from "@/lib/supabase/client";
import type { MemberRow } from "@/lib/types";

const tiers = ["None", "Darshan", "Gold", "Platinum", "Nri"];
type Funnel = "all" | "complete" | "incomplete";

export function MembersTable({ initial }: { initial: MemberRow[] }) {
  const router = useRouter();
  const [rows, setRows] = useState(initial);
  const [query, setQuery] = useState("");
  const [funnel, setFunnel] = useState<Funnel>("all");
  const [tier, setTier] = useState("all");
  const [message, setMessage] = useState("");
  const [ok, setOk] = useState(false);

  const visible = useMemo(() => {
    return rows.filter((row) => {
      if (funnel === "complete" && !row.profile_complete) return false;
      if (funnel === "incomplete" && row.profile_complete) return false;
      if (tier !== "all" && row.tier !== tier) return false;
      const places = (row.addresses ?? []).map((place) => `${place.label} ${place.address}`).join(" ");
      const hay = `${row.display_name} ${row.city} ${places} ${row.member_id} ${row.invoice_number ?? ""} ${row.tier} ${row.phone_e164}`.toLowerCase();
      const q = query.trim().toLowerCase();
      return !q || hay.includes(q);
    });
  }, [rows, query, funnel, tier]);

  async function save(row: MemberRow) {
    setMessage("");
    setOk(false);
    const supabase = createClient();
    const invoice = (row.invoice_number ?? "").trim();
    const amount = Number(row.amount_inr);
    if (invoice && (!row.purchased_on || !row.valid_from || !row.invoice_valid_until || !Number.isFinite(amount))) {
      setMessage("An invoice needs a purchase date, valid from, valid until, and an amount.");
      return;
    }
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
    if (invoice) {
      const purchase = {
        user_id: row.id,
        tier: row.tier,
        member_id: row.member_id,
        amount_inr: amount,
        invoice_number: invoice,
        purchased_on: row.purchased_on,
        valid_from: row.valid_from,
        valid_until: row.invoice_valid_until,
      };
      const saved = row.purchase_id
        ? await supabase.from("membership_purchases").update(purchase).eq("id", row.purchase_id)
        : await supabase.from("membership_purchases").insert(purchase).select("id").single();
      if (saved.error) {
        setMessage(saved.error.message);
        return;
      }
      const purchaseId = row.purchase_id ?? (saved.data && "id" in saved.data ? saved.data.id : undefined);
      if (purchaseId) {
        setRows((current) =>
          current.map((item) => (item.id === row.id ? { ...item, purchase_id: purchaseId } : item)),
        );
      }
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
      <div className="flex flex-wrap gap-2">
        <input
          value={query}
          onChange={(event) => setQuery(event.target.value)}
          placeholder="Search name, city, phone, or member ID"
          className={`${fieldClass} max-w-xs`}
        />
        {(["all", "complete", "incomplete"] as const).map((item) => (
          <button
            key={item}
            type="button"
            onClick={() => setFunnel(item)}
            className={
              funnel === item
                ? "rounded-md bg-zinc-900 px-3 py-2 text-sm text-white"
                : "rounded-md border border-zinc-200 bg-white px-3 py-2 text-sm text-zinc-600"
            }
          >
            {item === "all" ? "All" : item === "complete" ? "Complete" : "Incomplete"}
          </button>
        ))}
        {(["all", ...tiers] as const).map((item) => (
          <button
            key={item}
            type="button"
            onClick={() => setTier(item)}
            className={
              tier === item
                ? "rounded-md bg-zinc-900 px-3 py-2 text-sm text-white"
                : "rounded-md border border-zinc-200 bg-white px-3 py-2 text-sm text-zinc-600"
            }
          >
            {item === "all" ? "All tiers" : item}
          </button>
        ))}
      </div>
      {message ? <Alert tone={ok ? "ok" : "error"}>{message}</Alert> : null}
      <Card className="overflow-x-auto">
        <table className="w-full min-w-[1400px] text-left text-sm">
          <thead className="border-b border-zinc-200 bg-zinc-50 text-xs font-medium uppercase tracking-wide text-zinc-500">
            <tr>
              <th className="px-4 py-3">Member</th>
              <th className="px-4 py-3">Tier</th>
              <th className="px-4 py-3">Member ID</th>
              <th className="px-4 py-3">Valid until</th>
              <th className="px-4 py-3">Bought</th>
              <th className="px-4 py-3">Invoice</th>
              <th className="px-4 py-3">Amount</th>
              <th className="px-4 py-3">Valid from</th>
              <th className="px-4 py-3">Invoice until</th>
              <th className="px-4 py-3" />
            </tr>
          </thead>
          <tbody>
            {visible.length === 0 ? (
              <tr>
                <td colSpan={9} className="px-4 py-10 text-center text-sm text-zinc-500">
                  No members match.
                </td>
              </tr>
            ) : null}
            {visible.map((row) => (
              <tr key={row.id} className="border-t border-zinc-100">
                <td className="px-4 py-3">
                  <p className="font-medium">{row.display_name || "Unnamed"}</p>
                  <p className="text-xs text-zinc-500">{row.phone_e164 || "No phone yet"}</p>
                  <p className="text-xs text-zinc-400">{row.city || "No city"}</p>
                  {(row.addresses ?? []).length === 0 ? (
                    <p className="text-xs text-zinc-400">No places yet</p>
                  ) : (
                    row.addresses.map((place) => (
                      <p key={`${place.label}-${place.address}`} className="text-xs text-zinc-400">
                        {place.label}: {place.address}
                      </p>
                    ))
                  )}
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
                    {tiers.map((item) => (
                      <option key={item}>{item}</option>
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
                <td className="px-4 py-3">
                  <input
                    type="date"
                    value={(row.purchased_on ?? "").slice(0, 10)}
                    onChange={(event) =>
                      setRows((current) =>
                        current.map((item) =>
                          item.id === row.id ? { ...item, purchased_on: event.target.value } : item,
                        ),
                      )
                    }
                    className={fieldClass}
                  />
                </td>
                <td className="px-4 py-3">
                  <input
                    value={row.invoice_number ?? ""}
                    placeholder="HNV-2026-0001"
                    onChange={(event) =>
                      setRows((current) =>
                        current.map((item) =>
                          item.id === row.id ? { ...item, invoice_number: event.target.value } : item,
                        ),
                      )
                    }
                    className={fieldClass}
                  />
                </td>
                <td className="px-4 py-3">
                  <input
                    type="number"
                    min={0}
                    value={row.amount_inr ?? ""}
                    onChange={(event) =>
                      setRows((current) =>
                        current.map((item) =>
                          item.id === row.id
                            ? { ...item, amount_inr: event.target.value === "" ? null : Number(event.target.value) }
                            : item,
                        ),
                      )
                    }
                    className={fieldClass}
                  />
                </td>
                <td className="px-4 py-3">
                  <input
                    type="date"
                    value={(row.valid_from ?? "").slice(0, 10)}
                    onChange={(event) =>
                      setRows((current) =>
                        current.map((item) =>
                          item.id === row.id ? { ...item, valid_from: event.target.value } : item,
                        ),
                      )
                    }
                    className={fieldClass}
                  />
                </td>
                <td className="px-4 py-3">
                  <input
                    type="date"
                    value={(row.invoice_valid_until ?? "").slice(0, 10)}
                    onChange={(event) =>
                      setRows((current) =>
                        current.map((item) =>
                          item.id === row.id ? { ...item, invoice_valid_until: event.target.value } : item,
                        ),
                      )
                    }
                    className={fieldClass}
                  />
                </td>
                <td className="px-4 py-3 text-right">
                  <div className="flex items-center justify-end gap-2">
                    <Badge tone={row.profile_complete ? "green" : "amber"}>
                      {row.profile_complete ? "Complete" : "Incomplete"}
                    </Badge>
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
