"use client";

import { useRouter } from "next/navigation";
import { useState } from "react";
import { Alert, Badge, Button, Card, Field, fieldClass } from "@/components/ui";
import { formatWhen } from "@/lib/ops";
import { createClient } from "@/lib/supabase/client";
import type { LiveSource } from "@/lib/types";
import { parseYoutubeRef, watchUrl } from "@/lib/youtube-ref";

const blankDraft = { link: "", channel_name: "", official: false, follow_channel: false, title_match: "", priority: 100 };

export function LiveSources({ mandirId, initial }: { mandirId: string; initial: LiveSource[] }) {
  const router = useRouter();
  const [rows, setRows] = useState(initial);
  const [draft, setDraft] = useState(blankDraft);
  const [message, setMessage] = useState("");
  const [busy, setBusy] = useState(false);

  async function run(work: () => PromiseLike<{ error: { message: string } | null }>) {
    setBusy(true);
    setMessage("");
    const { error } = await work();
    setBusy(false);
    if (error) {
      setMessage(error.message);
      return false;
    }
    router.refresh();
    return true;
  }

  async function onAdd(event: React.FormEvent) {
    event.preventDefault();
    const ref = parseYoutubeRef(draft.link);
    if (!ref) {
      setMessage("Paste a YouTube video, live, or channel link.");
      return;
    }
    setBusy(true);
    setMessage("");
    const { data, error } = await createClient()
      .from("live_sources")
      .insert({
        mandir_id: mandirId,
        ...ref,
        channel_name: draft.channel_name.trim(),
        official: draft.official,
        follow_channel: draft.follow_channel || !ref.video_id,
        title_match: draft.title_match.trim(),
        priority: Number(draft.priority) || 100,
      })
      .select("*")
      .single();
    setBusy(false);
    if (error || !data) {
      setMessage(error?.message ?? "Could not add that source.");
      return;
    }
    const added = data as LiveSource;
    setRows((current) => [...current, added].sort((a, b) => a.priority - b.priority));
    setDraft(blankDraft);
    router.refresh();
  }

  async function patch(id: string, change: Partial<LiveSource>) {
    const saved = await run(() => createClient().from("live_sources").update(change).eq("id", id));
    if (saved) setRows((current) => current.map((row) => (row.id === id ? { ...row, ...change } : row)));
  }

  async function remove(row: LiveSource) {
    if (!confirm(`Remove ${row.channel_name || "this source"}?`)) return;
    const saved = await run(() => createClient().from("live_sources").delete().eq("id", row.id));
    if (saved) setRows((current) => current.filter((item) => item.id !== row.id));
  }

  const enabled = rows.filter((row) => row.enabled).length;

  return (
    <Card className="mt-6 overflow-hidden">
      <div className="border-b border-zinc-200 px-5 py-4">
        <p className="text-[11px] font-semibold uppercase tracking-[0.2em] text-amber-700">Live sources</p>
        <h2 className="mt-1 font-serif text-xl text-zinc-900">Where the live feed comes from</h2>
        <p className="mt-1 max-w-2xl text-sm text-zinc-500">
          {enabled > 0
            ? "The checker runs every 10 minutes. It sets Live now and the stream URL from the best source that is live: official first, then priority, then viewers."
            : "No sources yet. Live now and the stream URL above stay manual until you add one."}
        </p>
      </div>
      <ul className="divide-y divide-zinc-100">
        {rows.map((row) => (
          <li key={row.id} className="flex flex-wrap items-center gap-3 px-5 py-3">
            <span
              className={`h-2.5 w-2.5 shrink-0 rounded-full ${
                !row.enabled ? "bg-zinc-300" : row.last_error ? "bg-red-500" : row.last_live ? "bg-emerald-500" : "bg-zinc-400"
              }`}
            />
            <div className="min-w-0 flex-1">
              <div className="flex flex-wrap items-center gap-1.5">
                <a href={watchUrl(row)} target="_blank" rel="noreferrer" className="truncate text-sm font-medium hover:underline">
                  {row.channel_name || row.video_id || row.channel_id}
                </a>
                {row.official ? <Badge tone="blue">Official</Badge> : null}
                {row.follow_channel ? <Badge>Follows channel</Badge> : null}
                {row.title_match ? <Badge>Title has “{row.title_match}”</Badge> : null}
                {row.enabled && row.last_live ? <Badge tone="green">Live · {row.last_viewers ?? 0} watching</Badge> : null}
                {!row.enabled ? <Badge tone="amber">Off</Badge> : null}
              </div>
              <p className="truncate text-xs text-zinc-500">
                {row.last_error || row.last_title || row.note || "Not checked yet"}
                {row.last_checked_at ? ` · ${formatWhen(row.last_checked_at)}` : ""}
              </p>
              {row.note && (row.last_error || row.last_title) ? (
                <p className="truncate text-xs text-amber-700">{row.note}</p>
              ) : null}
            </div>
            <input
              type="number"
              aria-label="Priority"
              title="Priority. Lower wins."
              defaultValue={row.priority}
              onBlur={(event) => {
                const value = Number(event.target.value) || 100;
                if (value !== row.priority) patch(row.id, { priority: value });
              }}
              className={`${fieldClass} w-20`}
            />
            <label className="flex items-center gap-1.5 text-xs text-zinc-600">
              <input
                type="checkbox"
                checked={row.enabled}
                disabled={busy}
                onChange={(event) => patch(row.id, { enabled: event.target.checked })}
              />
              On
            </label>
            <Button type="button" variant="ghost" disabled={busy} onClick={() => remove(row)}>
              Remove
            </Button>
          </li>
        ))}
      </ul>
      <form onSubmit={onAdd} className="grid gap-4 border-t border-zinc-200 bg-zinc-50/60 px-5 py-4 sm:grid-cols-2">
        <Field label="YouTube link" hint="A live video, youtube.com/@channel, or a channel ID.">
          <input
            value={draft.link}
            onChange={(event) => setDraft({ ...draft, link: event.target.value })}
            placeholder="https://www.youtube.com/watch?v="
            className={fieldClass}
          />
        </Field>
        <Field label="Channel name" hint="Filled in by the checker if left blank.">
          <input
            value={draft.channel_name}
            onChange={(event) => setDraft({ ...draft, channel_name: event.target.value })}
            className={fieldClass}
          />
        </Field>
        <Field label="Title must include" hint="For channels that stream more than one mandir.">
          <input
            value={draft.title_match}
            onChange={(event) => setDraft({ ...draft, title_match: event.target.value })}
            placeholder="kalupur"
            className={fieldClass}
          />
        </Field>
        <Field label="Priority" hint="Lower wins when two sources are live.">
          <input
            type="number"
            value={draft.priority}
            onChange={(event) => setDraft({ ...draft, priority: Number(event.target.value) })}
            className={fieldClass}
          />
        </Field>
        <div className="flex flex-wrap items-center gap-5 text-sm sm:col-span-2">
          <label className="flex items-center gap-2 text-zinc-700">
            <input
              type="checkbox"
              checked={draft.official}
              onChange={(event) => setDraft({ ...draft, official: event.target.checked })}
            />
            Run by the temple
          </label>
          <label className="flex items-center gap-2 text-zinc-700">
            <input
              type="checkbox"
              checked={draft.follow_channel}
              onChange={(event) => setDraft({ ...draft, follow_channel: event.target.checked })}
            />
            Follow the channel when this video ends
          </label>
          <Button type="submit" disabled={busy || !draft.link.trim()} className="ml-auto">
            Add source
          </Button>
        </div>
        {message ? (
          <div className="sm:col-span-2">
            <Alert>{message}</Alert>
          </div>
        ) : null}
      </form>
    </Card>
  );
}
