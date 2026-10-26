"use client";

import { useRouter } from "next/navigation";
import { useState } from "react";
import { Alert, Button } from "@/components/ui";
import { createClient } from "@/lib/supabase/client";

export function ClaimOwner() {
  const router = useRouter();
  const [busy, setBusy] = useState(false);
  const [message, setMessage] = useState("");

  async function claim() {
    setBusy(true);
    setMessage("");
    const { data, error } = await createClient().rpc("bootstrap_staff");
    setBusy(false);
    if (error) {
      setMessage(error.message);
      return;
    }
    if (data !== true) {
      setMessage("Not available for this account. The team already has an owner, or this email is not on the bootstrap list.");
      return;
    }
    router.replace("/");
    router.refresh();
  }

  return (
    <div className="space-y-3">
      <Button type="button" onClick={claim} disabled={busy}>
        {busy ? "Checking…" : "Claim owner"}
      </Button>
      {message ? <Alert>{message}</Alert> : null}
    </div>
  );
}
