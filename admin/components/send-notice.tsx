"use client";

import { FunctionsHttpError } from "@supabase/supabase-js";
import { useRouter } from "next/navigation";
import { useState } from "react";
import { Alert, Button, Field, fieldClass } from "@/components/ui";
import { createClient } from "@/lib/supabase/client";

export function SendNotice() {
  const router = useRouter();
  const [title, setTitle] = useState("");
  const [body, setBody] = useState("");
  const [message, setMessage] = useState("");
  const [ok, setOk] = useState(false);
  const [busy, setBusy] = useState(false);

  async function onSubmit(event: React.FormEvent) {
    event.preventDefault();
    setMessage("");
    setOk(false);
    setBusy(true);
    try {
      const supabase = createClient();
      const { data, error } = await supabase.functions.invoke("send-notification", {
        body: { title, body },
      });
      if (error) {
        let detail = error.message;
        if (error instanceof FunctionsHttpError) {
          const payload = await error.context.json().catch(() => null);
          if (payload && typeof payload.error === "string") detail = payload.error;
        }
        throw new Error(detail);
      }
      const sent = Number(data?.sent ?? 0);
      const tokens = Number(data?.tokens ?? 0);
      setOk(true);
      setMessage(tokens === 0 ? "Saved. No phones are registered yet." : `Sent to ${sent} of ${tokens} phones.`);
      setTitle("");
      setBody("");
      router.refresh();
    } catch (error) {
      setOk(false);
      setMessage(error instanceof Error ? error.message : "Couldn't send that notice.");
    } finally {
      setBusy(false);
    }
  }

  return (
    <form onSubmit={onSubmit} className="space-y-4">
      <Field label="Title">
        <input
          required
          maxLength={80}
          value={title}
          onChange={(event) => setTitle(event.target.value)}
          placeholder="Aarti at Kashi"
          className={fieldClass}
        />
      </Field>
      <Field label="Message" hint="Goes to every signed-in phone, and stays in the app list.">
        <textarea
          required
          maxLength={400}
          rows={4}
          value={body}
          onChange={(event) => setBody(event.target.value)}
          placeholder="We will remind you before aarti."
          className={fieldClass}
        />
      </Field>
      <Button type="submit" disabled={busy}>
        {busy ? "Sending…" : "Send"}
      </Button>
      {message ? <Alert tone={ok ? "ok" : "error"}>{message}</Alert> : null}
    </form>
  );
}
