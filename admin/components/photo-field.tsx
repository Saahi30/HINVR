"use client";

import { useEffect, useMemo, useState } from "react";
import { Alert, Button, fieldClass } from "@/components/ui";
import { createClient } from "@/lib/supabase/client";
import { checkHttpsUrl } from "@/lib/urls";

const IMAGE_TYPES = /^image\/(jpeg|png|webp|gif)$/;

export function checkPhotoFile(file: File): string {
  if (file.size > 5 * 1024 * 1024) return "Max 5 MB.";
  if (!IMAGE_TYPES.test(file.type)) return "Use JPG, PNG, WebP, or GIF.";
  return "";
}

/** Uploads a photo to the public media bucket under folder/<uuid>.<ext> and returns its URL. */
export async function uploadPhoto(file: File, folder: string): Promise<string> {
  const problem = checkPhotoFile(file);
  if (problem) throw new Error(problem);
  const supabase = createClient();
  const ext = file.type === "image/jpeg" ? "jpg" : file.type.split("/")[1];
  const path = `${folder}/${crypto.randomUUID()}.${ext}`;
  const { error } = await supabase.storage.from("media").upload(path, file, { contentType: file.type, upsert: false });
  if (error) throw error;
  return supabase.storage.from("media").getPublicUrl(path).data.publicUrl;
}

/**
 * Photo picker. With `onPendingFile`, nothing uploads until the form saves (so a cancelled
 * new mandir leaves no orphaned file); without it, the file uploads straight to `folder`.
 */
export function PhotoField({
  label,
  value,
  folder,
  onChange,
  pendingFile,
  onPendingFile,
}: {
  label: string;
  value: string;
  folder: string;
  onChange: (url: string) => void;
  pendingFile?: File | null;
  onPendingFile?: (file: File | null) => void;
}) {
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);
  const preview = useMemo(() => (pendingFile ? URL.createObjectURL(pendingFile) : ""), [pendingFile]);
  useEffect(() => {
    if (!preview) return;
    return () => URL.revokeObjectURL(preview);
  }, [preview]);

  async function onFile(file: File | undefined) {
    if (!file) return;
    setError("");
    const problem = checkPhotoFile(file);
    if (problem) {
      setError(problem);
      return;
    }
    if (onPendingFile) {
      onPendingFile(file);
      return;
    }
    setBusy(true);
    try {
      onChange(await uploadPhoto(file, folder));
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : "Upload failed.");
    } finally {
      setBusy(false);
    }
  }

  const shown = preview || value;
  const urlProblem = pendingFile ? "" : checkHttpsUrl(value, "Photo URL");

  return (
    <div>
      <p className="mb-1.5 text-sm font-medium text-zinc-700">{label}</p>
      {shown ? (
        // eslint-disable-next-line @next/next/no-img-element
        <img src={shown} alt="" className="h-40 w-full rounded-lg object-cover" />
      ) : (
        <div className="flex h-40 items-center justify-center rounded-lg border border-dashed border-zinc-300 bg-zinc-50 text-sm text-zinc-500">
          No photo
        </div>
      )}
      {pendingFile ? <p className="mt-2 text-xs text-zinc-500">Uploads when you save.</p> : null}
      <div className="mt-3 flex flex-wrap gap-2">
        <label className="inline-flex cursor-pointer items-center rounded-md bg-zinc-900 px-3 py-2 text-sm font-medium text-white">
          {busy ? "Uploading…" : "Upload"}
          <input
            type="file"
            accept="image/jpeg,image/png,image/webp,image/gif"
            className="hidden"
            disabled={busy}
            onChange={(event) => {
              void onFile(event.target.files?.[0]);
              event.target.value = "";
            }}
          />
        </label>
        {shown ? (
          <Button
            type="button"
            variant="secondary"
            onClick={() => {
              onPendingFile?.(null);
              onChange("");
            }}
          >
            Remove
          </Button>
        ) : null}
      </div>
      {pendingFile ? null : (
        <input
          type="url"
          value={value}
          onChange={(event) => onChange(event.target.value)}
          placeholder="Or paste an https image URL"
          aria-invalid={Boolean(urlProblem)}
          className={`${fieldClass} mt-3`}
        />
      )}
      {error || urlProblem ? (
        <div className="mt-2">
          <Alert>{error || urlProblem}</Alert>
        </div>
      ) : null}
    </div>
  );
}
