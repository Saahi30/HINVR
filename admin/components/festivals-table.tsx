"use client";

import { useRouter } from "next/navigation";
import { useState } from "react";
import { Alert, Badge, Button, Card, Field, fieldClass } from "@/components/ui";
import { slugify } from "@/lib/slug";
import { createClient } from "@/lib/supabase/client";
import { FESTIVAL_KINDS, type Festival, type FestivalKind } from "@/lib/types";

/** What each kind adds to the sanctum, as the headset does it. */
const KIND_LABELS: Record<FestivalKind, string> = {
  deepmala: "Deepmala · a tray of diyas to light",
  abhishek: "Abhishek · water poured for the lingam",
  modak: "Modak · naivedya for Ganesha",
  hibiscus: "Hibiscus · red flowers for Devi",
};

const blank = (): Festival => ({
  id: "",
  kind: "deepmala",
  name: "",
  name_hi: "",
  starts_on: "",
  ends_on: "",
  enabled: false,
  notes: "",
});

export function FestivalsTable({ initial, canDelete }: { initial: Festival[]; canDelete: boolean }) {
  const router = useRouter();
  const [rows, setRows] = useState(initial);
  const [draft, setDraft] = useState<Festival>(blank());
  const [message, setMessage] = useState("");
  const [ok, setOk] = useState(false);
  const [busy, setBusy] = useState(false);

  function problem(row: Festival): string {
    if (!row.name.trim()) return "A festival needs a name.";
    if (!row.starts_on || !row.ends_on) return "Set both dates.";
    if (row.ends_on < row.starts_on) return "The last day is before the first.";
    const days = (Date.parse(row.ends_on) - Date.parse(row.starts_on)) / 86_400_000;
    if (days > 15) return "A festival can run at most 16 days.";
    return "";
  }

  function report(text: string, good = false) {
    setMessage(text);
    setOk(good);
  }

  async function save(row: Festival) {
    const issue = problem(row);
    if (issue) return report(issue);
    setBusy(true);
    report("");
    const payload = {
      kind: row.kind,
      name: row.name.trim(),
      name_hi: row.name_hi.trim(),
      starts_on: row.starts_on,
      ends_on: row.ends_on,
      enabled: row.enabled,
      notes: row.notes.trim(),
    };
    const { error } = await createClient().from("festivals").update(payload).eq("id", row.id);
    setBusy(false);
    if (error) return report(error.message);
    report(`${row.name} saved.`, true);
    router.refresh();
  }

  async function add(event: React.FormEvent) {
    event.preventDefault();
    const issue = problem(draft);
    if (issue) return report(issue);
    const id = slugify(`${draft.name} ${draft.starts_on.slice(0, 4)}`);
    setBusy(true);
    report("");
    const row = { ...draft, id, name: draft.name.trim(), name_hi: draft.name_hi.trim(), notes: draft.notes.trim() };
    const { error } = await createClient().from("festivals").insert(row);
    setBusy(false);
    if (error) return report(error.message);
    setRows((current) => [...current, row].sort((a, b) => a.starts_on.localeCompare(b.starts_on)));
    setDraft(blank());
    report(`${row.name} added. It stays off until you enable it.`, true);
    router.refresh();
  }

  async function remove(row: Festival) {
    if (!confirm(`Delete ${row.name}?`)) return;
    const { error } = await createClient().from("festivals").delete().eq("id", row.id);
    if (error) return report(error.message);
    setRows((current) => current.filter((item) => item.id !== row.id));
    router.refresh();
  }

  function patch(id: string, change: Partial<Festival>) {
    setRows((current) => current.map((row) => (row.id === id ? { ...row, ...change } : row)));
  }

  return (
    <div className="space-y-6">
      {message ? <Alert tone={ok ? "ok" : "error"}>{message}</Alert> : null}
      <div className="space-y-3">
        {rows.length === 0 ? <p className="text-sm text-zinc-500">No festivals yet.</p> : null}
        {rows.map((row) => (
          <Card key={row.id} className="space-y-3 p-4">
            <div className="flex flex-wrap items-center gap-2">
              <h2 className="font-serif text-lg text-zinc-900">{row.name || row.id}</h2>
              {row.enabled ? <Badge tone="green">On</Badge> : <Badge>Off</Badge>}
              <span className="text-xs text-zinc-500">{KIND_LABELS[row.kind]}</span>
            </div>
            <FestivalFields row={row} onChange={(change) => patch(row.id, change)} />
            {row.notes ? <p className="text-xs text-amber-800">{row.notes}</p> : null}
            <div className="flex flex-wrap gap-2">
              <Button type="button" disabled={busy} onClick={() => save(row)}>
                Save
              </Button>
              {canDelete ? (
                <Button type="button" variant="danger" disabled={busy} onClick={() => remove(row)}>
                  Delete
                </Button>
              ) : null}
            </div>
          </Card>
        ))}
      </div>
      <Card className="p-4">
        <form onSubmit={add} className="space-y-3">
          <h2 className="font-serif text-lg text-zinc-900">Add a festival</h2>
          <FestivalFields row={draft} onChange={(change) => setDraft((current) => ({ ...current, ...change }))} />
          <Button type="submit" disabled={busy}>
            Add festival
          </Button>
        </form>
      </Card>
    </div>
  );
}

function FestivalFields({ row, onChange }: { row: Festival; onChange: (change: Partial<Festival>) => void }) {
  return (
    <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
      <Field label="Name">
        <input value={row.name} onChange={(event) => onChange({ name: event.target.value })} className={fieldClass} />
      </Field>
      <Field label="Name in Hindi">
        <input value={row.name_hi} onChange={(event) => onChange({ name_hi: event.target.value })} className={fieldClass} />
      </Field>
      <Field label="Puja in the sanctum">
        <select
          value={row.kind}
          onChange={(event) => onChange({ kind: event.target.value as FestivalKind })}
          className={fieldClass}
        >
          {FESTIVAL_KINDS.map((kind) => (
            <option key={kind} value={kind}>
              {KIND_LABELS[kind]}
            </option>
          ))}
        </select>
      </Field>
      <Field label="First day (IST)">
        <input
          type="date"
          value={row.starts_on}
          onChange={(event) => onChange({ starts_on: event.target.value })}
          className={fieldClass}
        />
      </Field>
      <Field label="Last day (IST)">
        <input
          type="date"
          value={row.ends_on}
          onChange={(event) => onChange({ ends_on: event.target.value })}
          className={fieldClass}
        />
      </Field>
      <label className="flex items-center gap-2 self-end pb-2 text-sm text-zinc-700">
        <input type="checkbox" checked={row.enabled} onChange={(event) => onChange({ enabled: event.target.checked })} />
        Enabled
      </label>
      <Field label="Notes">
        <input value={row.notes} onChange={(event) => onChange({ notes: event.target.value })} className={fieldClass} />
      </Field>
    </div>
  );
}
