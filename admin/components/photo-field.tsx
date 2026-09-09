"use client";

import { useState } from "react";
import { Alert, Button, fieldClass } from "@/components/ui";
import { createClient } from "@/lib/supabase/client";

export function PhotoField({
  label,
  value,
  folder,
  onChange,
}: {
  label: string;
  value: string;
  folder: string;
  onChange: (url: string) => void;
}) {
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);

  async function onFile(file: File | undefined) {
    if (!file) return;
    setError("");
    if (file.size > 5 * 1024 * 1024) {
      setError("Max 5 MB.");
      return;
    }
    if (!/^image\/(jpeg|png|webp|gif)$/.test(file.type)) {
      setError("Use JPG, PNG, WebP, or GIF.");
      return;
    }
    setBusy(true);
    try {
      const supabase = createClient();
      const ext = file.type === "image/jpeg" ? "jpg" : file.type.split("/")[1];
      const path = `${folder}/${crypto.randomUUID()}.${ext}`;
      const { error: uploadError } = await supabase.storage.from("media").upload(path, file, {
        contentType: file.type,
        upsert: false,
      });
      if (uploadError) throw uploadError;
      const { data } = supabase.storage.from("media").getPublicUrl(path);
      onChange(data.publicUrl);
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : "Upload failed.");
    } finally {
      setBusy(false);
    }
  }

  return (
    <div>
      <p className="mb-1.5 text-sm font-medium text-zinc-700">{label}</p>
      {value ? (
        // eslint-disable-next-line @next/next/no-img-element
        <img src={value} alt="" className="h-40 w-full rounded-lg object-cover" />
      ) : (
        <div className="flex h-40 items-center justify-center rounded-lg border border-dashed border-zinc-300 bg-zinc-50 text-sm text-zinc-500">
          No photo
        </div>
      )}
      <div className="mt-3 flex flex-wrap gap-2">
        <label className="inline-flex cursor-pointer items-center rounded-md bg-zinc-900 px-3 py-2 text-sm font-medium text-white">
          {busy ? "Uploading…" : "Upload"}
          <input
            type="file"
            accept="image/jpeg,image/png,image/webp,image/gif"
            className="hidden"
            disabled={busy}
            onChange={(event) => onFile(event.target.files?.[0])}
          />
        </label>
        {value ? (
          <Button type="button" variant="secondary" onClick={() => onChange("")}>
            Remove
          </Button>
        ) : null}
      </div>
      <input
        value={value}
        onChange={(event) => onChange(event.target.value)}
        placeholder="Or paste an https image URL"
        className={`${fieldClass} mt-3`}
      />
      {error ? <div className="mt-2"><Alert>{error}</Alert></div> : null}
    </div>
  );
}
