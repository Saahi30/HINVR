"use client";

import { FunctionsHttpError } from "@supabase/supabase-js";
import { useRouter } from "next/navigation";
import { useState } from "react";
import { Dialog } from "@/components/dialog";
import { Alert, Button, Field, fieldClass } from "@/components/ui";
import { createClient } from "@/lib/supabase/client";

async function invoke(body: Record<string, unknown>) {
  const { data, error } = await createClient().functions.invoke("send-notification", { body });
  if (error) {
    let detail = error.message;
    if (error instanceof FunctionsHttpError) {
      const payload = await error.context.json().catch(() => null);
      if (payload && typeof payload.error === "string") detail = payload.error;
    }
    throw new Error(detail);
  }
  return data as Record<string, unknown>;
}

export function SendNotice() {
  const router = useRouter();
  const [title, setTitle] = useState("");
  const [body, setBody] = useState("");
  const [message, setMessage] = useState("");
  const [ok, setOk] = useState(false);
  const [busy, setBusy] = useState(false);
  const [audience, setAudience] = useState<number | null>(null);

  async function review(event: React.FormEvent) {
    event.preventDefault();
    setMessage("");
    setOk(false);
    setBusy(true);
    try {
      const data = await invoke({ preview: true });
      setAudience(Number(data?.tokens ?? 0));
    } catch (error) {
      setMessage(error instanceof Error ? error.message : "Couldn't count phones.");
    } finally {
      setBusy(false);
    }
  }

  async function send() {
    setBusy(true);
    try {
      const data = await invoke({ title, body });
      const sent = Number(data?.sent ?? 0);
      const tokens = Number(data?.tokens ?? 0);
      setOk(true);
      setMessage(tokens === 0 ? "Saved. No phones are registered yet." : `Sent to ${sent} of ${tokens} phones.`);
      setTitle("");
      setBody("");
      setAudience(null);
      router.refresh();
    } catch (error) {
      setOk(false);
      setMessage(error instanceof Error ? error.message : "Couldn't send that notice.");
      setAudience(null);
    } finally {
      setBusy(false);
    }
  }

  return (
    <form onSubmit={review} className="space-y-4">
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
      <Field label="Message" hint="Goes to every signed-in phone, and stays in the app list. You confirm before it sends.">
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
      <Button type="submit" disabled={busy || !title.trim() || !body.trim()}>
        {busy && audience == null ? "Counting phones…" : "Review and send"}
      </Button>
      {message ? <Alert tone={ok ? "ok" : "error"}>{message}</Alert> : null}
      <Dialog open={audience != null} title="Send to every phone?" onClose={() => (busy ? undefined : setAudience(null))}>
        <p className="text-sm text-zinc-600">
          {audience === 0
            ? "No phones are registered yet. The notice is saved to the app list only."
            : `This goes to ${audience} phone${audience === 1 ? "" : "s"} right away. It cannot be recalled.`}
        </p>
        <div className="mt-4 rounded-xl border border-zinc-200 bg-zinc-50 p-4" aria-label="Preview">
          <p className="text-[11px] font-medium tracking-wide text-zinc-500 uppercase">HINVR · now</p>
          <p className="mt-1 text-sm font-semibold text-zinc-900">{title}</p>
          <p className="mt-1 text-sm whitespace-pre-line text-zinc-600">{body}</p>
        </div>
        <div className="mt-5 flex justify-end gap-2">
          <Button type="button" variant="secondary" disabled={busy} onClick={() => setAudience(null)}>
            Cancel
          </Button>
          <Button type="button" disabled={busy} onClick={send}>
            {busy ? "Sending…" : audience === 0 ? "Save notice" : `Send to ${audience}`}
          </Button>
        </div>
      </Dialog>
    </form>
  );
}
