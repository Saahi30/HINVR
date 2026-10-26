"use client";

import { useEffect, useId, useRef } from "react";

/** Modal dialog: focus moves in, Escape and the backdrop close it, focus returns on close. */
export function Dialog({
  open,
  title,
  onClose,
  children,
  wide = false,
}: {
  open: boolean;
  title: string;
  onClose: () => void;
  children: React.ReactNode;
  wide?: boolean;
}) {
  const titleId = useId();
  const panel = useRef<HTMLDivElement>(null);
  const closeRef = useRef(onClose);
  useEffect(() => {
    closeRef.current = onClose;
  });

  useEffect(() => {
    if (!open) return;
    const previous = document.activeElement as HTMLElement | null;
    const first = panel.current?.querySelector<HTMLElement>("input, select, textarea, button");
    first?.focus();
    function onKey(event: KeyboardEvent) {
      if (event.key === "Escape") closeRef.current();
    }
    document.addEventListener("keydown", onKey);
    return () => {
      document.removeEventListener("keydown", onKey);
      previous?.focus();
    };
  }, [open]);

  if (!open) return null;
  return (
    <div className="fixed inset-0 z-50 flex items-start justify-center overflow-y-auto bg-black/40 px-4 py-10">
      <button type="button" aria-label="Close" className="fixed inset-0 cursor-default" onClick={onClose} />
      <div
        ref={panel}
        role="dialog"
        aria-modal="true"
        aria-labelledby={titleId}
        className={`relative w-full ${wide ? "max-w-2xl" : "max-w-md"} rounded-xl border border-zinc-200 bg-white p-5 shadow-xl`}
      >
        <h2 id={titleId} className="text-base font-semibold text-zinc-900">
          {title}
        </h2>
        <div className="mt-4">{children}</div>
      </div>
    </div>
  );
}
