"use client";

import { FunctionsHttpError } from "@supabase/supabase-js";
import { useRouter } from "next/navigation";
import { useState } from "react";
import { Alert, Button } from "@/components/ui";
import { createClient } from "@/lib/supabase/client";

export function LiveCheckButton() {
  const router = useRouter();
  const [busy, setBusy] = useState(false);
  const [message, setMessage] = useState("");
  const [ok, setOk] = useState(false);

  async function onClick() {
    setBusy(true);
    setMessage("");
    try {
      const { data, error } = await createClient().functions.invoke("live-check", { body: { find: true } });
      if (error) {
        let detail = error.message;
        if (error instanceof FunctionsHttpError) {
          const payload = await error.context.json().catch(() => null);
          if (payload && typeof payload.error === "string") detail = payload.error;
        }
        throw new Error(detail);
      }
      setOk(true);
      setMessage(
        `${Number(data?.live ?? 0)} of ${Number(data?.mandirs ?? 0)} mandirs live` +
          (Number(data?.replaced ?? 0) > 0 ? ` · replaced ${Number(data.replaced)} dead feeds` : "") +
          (data?.quota
            ? ` · ${Number(data.quota.last?.units ?? 0)} units this run, ${Number(data.quota.used)}/${Number(data.quota.limit)} today`
            : "") +
          ".",
      );
      router.refresh();
    } catch (error) {
      setOk(false);
      setMessage(error instanceof Error ? error.message : "Couldn't check feeds.");
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="flex items-center gap-3">
      {message ? <Alert tone={ok ? "ok" : "error"}>{message}</Alert> : null}
      <Button type="button" variant="secondary" onClick={onClick} disabled={busy}>
        {busy ? "Checking…" : "Check feeds now"}
      </Button>
    </div>
  );
}
