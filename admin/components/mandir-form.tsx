"use client";

import { useRouter } from "next/navigation";
import { useState } from "react";
import { PhotoField } from "@/components/photo-field";
import { Alert, Button, Card, Field, fieldClass } from "@/components/ui";
import { slugify } from "@/lib/slug";
import { createClient } from "@/lib/supabase/client";
import { MANDIR_SCENES, type Mandir } from "@/lib/types";

export function MandirForm({ initial, isNew }: { initial: Mandir; isNew: boolean }) {
  const router = useRouter();
  const [row, setRow] = useState(initial);
  const [message, setMessage] = useState("");
  const [busy, setBusy] = useState(false);

  function patch<K extends keyof Mandir>(key: K, value: Mandir[K]) {
    setRow((current) => ({ ...current, [key]: value }));
  }

  async function onSubmit(event: React.FormEvent) {
    event.preventDefault();
    setMessage("");
    const id = isNew ? slugify(row.id || row.name) : row.id;
    if (!id || !row.name.trim()) {
      setMessage("Name and slug are required.");
      return;
    }
    setBusy(true);
    const payload = {
      id,
      name: row.name.trim(),
      place: row.place.trim(),
      city: row.city.trim(),
      scene: row.scene,
      photo_url: row.photo_url.trim(),
      live: row.live,
      vr: row.vr,
      pass_accepted: row.pass_accepted,
      next_aarti: row.next_aarti?.trim() || null,
      timings: row.timings.trim(),
      updated_label: row.updated_label.trim() || "Updated just now",
      live_url: row.live_url.trim(),
      vr_url: row.vr_url.trim(),
      sort_order: Number(row.sort_order) || 0,
      published: row.published,
    };
    try {
      const supabase = createClient();
      const query = isNew
        ? supabase.from("mandirs").insert(payload)
        : supabase.from("mandirs").update(payload).eq("id", id);
      const { error } = await query;
      if (error) throw error;
      router.push("/mandirs");
      router.refresh();
    } catch (error) {
      setMessage(error instanceof Error ? error.message : "Could not save.");
    } finally {
      setBusy(false);
    }
  }

  async function onDelete() {
    if (!confirm(`Delete ${row.name} from the catalog?`)) return;
    setBusy(true);
    try {
      const supabase = createClient();
      const { error } = await supabase.from("mandirs").delete().eq("id", row.id);
      if (error) throw error;
      router.push("/mandirs");
      router.refresh();
    } catch (error) {
      setMessage(error instanceof Error ? error.message : "Could not delete.");
      setBusy(false);
    }
  }

  return (
    <form onSubmit={onSubmit} className="grid gap-6 xl:grid-cols-[minmax(0,1fr)_20rem]">
      <Card className="space-y-4 p-5">
        {isNew ? (
          <Field label="Slug" hint="Used in the app URL. Lowercase, no spaces.">
            <input
              value={row.id}
              onChange={(event) => patch("id", event.target.value)}
              placeholder={slugify(row.name) || "tirupati"}
              className={fieldClass}
            />
          </Field>
        ) : (
          <p className="text-xs text-zinc-500">Slug · {row.id}</p>
        )}
        <Field label="Name">
          <input required value={row.name} onChange={(event) => patch("name", event.target.value)} className={fieldClass} />
        </Field>
        <div className="grid gap-4 sm:grid-cols-2">
          <Field label="City">
            <input value={row.city} onChange={(event) => patch("city", event.target.value)} className={fieldClass} />
          </Field>
          <Field label="Place">
            <input value={row.place} onChange={(event) => patch("place", event.target.value)} className={fieldClass} />
          </Field>
        </div>
        <Field label="Fallback illustration">
          <select value={row.scene} onChange={(event) => patch("scene", event.target.value)} className={fieldClass}>
            {MANDIR_SCENES.map((scene) => (
              <option key={scene}>{scene}</option>
            ))}
          </select>
        </Field>
        <Field label="Official timings">
          <textarea rows={3} value={row.timings} onChange={(event) => patch("timings", event.target.value)} className={fieldClass} />
        </Field>
        <Field label="Live stream URL">
          <input
            value={row.live_url}
            onChange={(event) => patch("live_url", event.target.value)}
            placeholder="https://youtube.com/watch?v="
            className={fieldClass}
          />
        </Field>
        <Field label="VR / 360 URL">
          <input
            value={row.vr_url}
            onChange={(event) => patch("vr_url", event.target.value)}
            placeholder="https://"
            className={fieldClass}
          />
        </Field>
        <div className="grid gap-4 sm:grid-cols-2">
          <Field label="Next aarti">
            <input
              value={row.next_aarti ?? ""}
              onChange={(event) => patch("next_aarti", event.target.value)}
              placeholder="4:30 PM"
              className={fieldClass}
            />
          </Field>
          <Field label="Freshness label">
            <input value={row.updated_label} onChange={(event) => patch("updated_label", event.target.value)} className={fieldClass} />
          </Field>
        </div>
      </Card>
      <aside className="space-y-4">
        <Card className="p-5">
          <PhotoField
            label="Card photo"
            value={row.photo_url}
            folder={`mandirs/${row.id || "new"}`}
            onChange={(url) => patch("photo_url", url)}
          />
        </Card>
        <Card className="space-y-3 p-5 text-sm">
          <Toggle label="Published" checked={row.published} onChange={(value) => patch("published", value)} />
          <Toggle label="Live now" checked={row.live} onChange={(value) => patch("live", value)} />
          <Toggle label="VR available" checked={row.vr} onChange={(value) => patch("vr", value)} />
          <Toggle label="Pass accepted" checked={row.pass_accepted} onChange={(value) => patch("pass_accepted", value)} />
          <Field label="Sort order">
            <input
              type="number"
              value={row.sort_order}
              onChange={(event) => patch("sort_order", Number(event.target.value))}
              className={fieldClass}
            />
          </Field>
        </Card>
        {message ? <Alert>{message}</Alert> : null}
        <Button type="submit" disabled={busy} className="w-full">
          {busy ? "Saving…" : isNew ? "Create mandir" : "Save changes"}
        </Button>
        {!isNew ? (
          <Button type="button" variant="danger" onClick={onDelete} className="w-full">
            Delete mandir
          </Button>
        ) : null}
      </aside>
    </form>
  );
}

function Toggle({
  label,
  checked,
  onChange,
}: {
  label: string;
  checked: boolean;
  onChange: (value: boolean) => void;
}) {
  return (
    <label className="flex items-center justify-between gap-3">
      <span className="text-zinc-700">{label}</span>
      <input type="checkbox" checked={checked} onChange={(event) => onChange(event.target.checked)} />
    </label>
  );
}
