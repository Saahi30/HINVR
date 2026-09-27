"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { Alert, Badge, Button, Card } from "@/components/ui";
import { createClient } from "@/lib/supabase/client";
import { formatWhen } from "@/lib/ops";

export type CardRequest = {
  id: string;
  user_id: string;
  member_id: string;
  ship_name: string;
  ship_address: string;
  status: "waitlist" | "printing" | "shipped" | "cancelled";
  created_at: string;
};

const nextStatus = {
  waitlist: "printing",
  printing: "shipped",
  shipped: "shipped",
  cancelled: "cancelled",
} as const;

export function CardWaitlist({ initial }: { initial: CardRequest[] }) {
  const router = useRouter();
  const [rows, setRows] = useState(initial);
  const [message, setMessage] = useState("");
  const [ok, setOk] = useState(false);
  const [busyId, setBusyId] = useState<string | null>(null);
  const open = rows.filter((row) => row.status === "waitlist" || row.status === "printing");

  async function advance(row: CardRequest, status: CardRequest["status"]) {
    setBusyId(row.id);
    setMessage("");
    setOk(false);
    const supabase = createClient();
    const { error } = await supabase.from("physical_card_requests").update({ status }).eq("id", row.id);
    setBusyId(null);
    if (error) {
      setMessage(error.message);
      return;
    }
    setRows((current) => current.map((item) => (item.id === row.id ? { ...item, status } : item)));
    setOk(true);
    setMessage(status === "cancelled" ? "Taken off the list." : "Card request updated.");
    router.refresh();
  }

  if (rows.length === 0) {
    return (
      <Card className="p-8 text-center text-sm text-zinc-500">
        Nobody has asked for a physical card yet. Gold and above can join the list from the pass.
      </Card>
    );
  }

  return (
    <div className="space-y-3">
      {message ? <Alert tone={ok ? "ok" : "error"}>{message}</Alert> : null}
      <p className="text-sm text-zinc-500">
        {open.length === 0 ? "No cards waiting." : `${open.length} waiting to print or send.`} The phone QR stays the
        credential. This list is only the card.
      </p>
      <div className="space-y-3">
        {rows.map((row) => (
          <Card key={row.id} className="p-4">
            <div className="flex flex-wrap items-start justify-between gap-3">
              <div>
                <p className="font-medium">{row.ship_name || "Member"}</p>
                <p className="mt-1 text-sm text-zinc-500">
                  {[row.member_id, formatWhen(row.created_at)].filter(Boolean).join(" · ")}
                </p>
                <p className="mt-2 max-w-xl text-sm text-zinc-700">{row.ship_address}</p>
              </div>
          <Badge
            tone={
              row.status === "waitlist" || row.status === "printing"
                ? "amber"
                : row.status === "cancelled"
                  ? "zinc"
                  : "green"
            }
          >
            {row.status}
          </Badge>
            </div>
            {row.status === "waitlist" || row.status === "printing" ? (
              <div className="mt-3 flex flex-wrap gap-2">
                <Button
                  type="button"
                  variant="secondary"
                  disabled={busyId === row.id}
                  onClick={() => advance(row, nextStatus[row.status])}
                >
                  {row.status === "waitlist" ? "Mark printing" : "Mark sent"}
                </Button>
                <Button type="button" variant="danger" disabled={busyId === row.id} onClick={() => advance(row, "cancelled")}>
                  Cancel
                </Button>
              </div>
            ) : null}
          </Card>
        ))}
      </div>
    </div>
  );
}
