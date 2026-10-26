"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import { cachedPassKeys, KEY_REFRESH_MS, refreshPassKeys } from "@/lib/pass-keys";
import { readQueue, readSetting, sendCheckIn, writeQueue, writeSetting, type QueuedScan } from "@/lib/check-in";
import { formatDate, tierLabel } from "@/lib/membership";
import { formatWhen } from "@/lib/ops";
import { classifyScan } from "@/lib/pass-code";
import { PassTokenError, verifyPassToken } from "@/lib/pass-token";
import { createClient } from "@/lib/supabase/client";
import type { CheckInMember, CheckInResult } from "@/lib/types";

type Tone = "green" | "amber" | "red";

type Shown = {
  tone: Tone;
  title: string;
  detail?: string;
  member?: Partial<CheckInMember> & { display_name?: string };
  /** What confirming does, when the desk must decide. */
  confirm?: { label: string; run: () => void };
  autoClose: boolean;
};

type Visit = { id: string; place: string; created_at: string; name: string; method?: string; flag?: string };

const PLACE_KEY = "hinvr.scan.place";
const COOLDOWN_MS = 8000;
const AUTO_CLOSE_MS = 3500;

const tones: Record<Tone, { panel: string; mark: string }> = {
  green: { panel: "bg-emerald-700", mark: "✓" },
  amber: { panel: "bg-amber-600", mark: "!" },
  red: { panel: "bg-red-700", mark: "✕" },
};

function beep(tone: Tone) {
  try {
    const AudioCtx = window.AudioContext || (window as unknown as { webkitAudioContext?: typeof AudioContext }).webkitAudioContext;
    if (!AudioCtx) return;
    const ctx = new AudioCtx();
    const notes = tone === "green" ? [880, 1320] : tone === "amber" ? [660, 660] : [220, 180];
    notes.forEach((frequency, index) => {
      const osc = ctx.createOscillator();
      const gain = ctx.createGain();
      osc.frequency.value = frequency;
      osc.type = tone === "red" ? "square" : "sine";
      gain.gain.value = 0.08;
      osc.connect(gain).connect(ctx.destination);
      const start = ctx.currentTime + index * 0.16;
      osc.start(start);
      osc.stop(start + 0.13);
    });
    setTimeout(() => void ctx.close().catch(() => {}), 800);
  } catch {
    // No audio: the panel still shows.
  }
  try {
    navigator.vibrate?.(tone === "green" ? 80 : tone === "amber" ? [80, 60, 80] : [200, 80, 200]);
  } catch {
    // Not supported.
  }
}

export function PassScanner() {
  const [place, setPlace] = useState("HINVR desk");
  const [note, setNote] = useState("");
  const [places, setPlaces] = useState<string[]>([]);
  const [cameraOn, setCameraOn] = useState(false);
  const [cameraError, setCameraError] = useState("");
  const [torch, setTorch] = useState<{ supported: boolean; on: boolean }>({ supported: false, on: false });
  const [manual, setManual] = useState("");
  const [busy, setBusy] = useState(false);
  const [shown, setShown] = useState<Shown | null>(null);
  const [online, setOnline] = useState(true);
  const [queued, setQueued] = useState(0);
  const [attention, setAttention] = useState<string[]>([]);
  const [recent, setRecent] = useState<Visit[]>([]);
  const [keyInfo, setKeyInfo] = useState({ count: 0, stale: true });

  const keys = useRef<Map<string, JsonWebKey>>(new Map());
  const lastSeen = useRef(new Map<string, number>());
  const scanner = useRef<{
    stop: () => Promise<void>;
    getRunningTrackCameraCapabilities: () => { torchFeature: () => { isSupported: () => boolean; apply: (on: boolean) => Promise<void> } };
  } | null>(null);
  const busyRef = useRef(false);
  const shownRef = useRef<Shown | null>(null);
  const handleRef = useRef<(raw: string, fromCamera: boolean) => void>(() => {});
  const placeRef = useRef(place);
  const noteRef = useRef(note);

  useEffect(() => {
    placeRef.current = place;
    noteRef.current = note;
    shownRef.current = shown;
  });

  const show = useCallback((next: Shown) => {
    beep(next.tone);
    setShown(next);
  }, []);

  useEffect(() => {
    if (!shown?.autoClose) return;
    const timer = setTimeout(() => setShown(null), AUTO_CLOSE_MS);
    return () => clearTimeout(timer);
  }, [shown]);

  const loadKeys = useCallback(async () => {
    const loaded = navigator.onLine ? await refreshPassKeys(createClient()) : cachedPassKeys();
    keys.current = loaded.keys;
    setKeyInfo({ count: loaded.keys.size, stale: !loaded.fetchedAt || Date.now() - loaded.fetchedAt > KEY_REFRESH_MS * 4 });
  }, []);

  const flush = useCallback(async () => {
    const pending = readQueue();
    if (pending.length === 0) return;
    const supabase = createClient();
    const left: QueuedScan[] = [];
    const notes: string[] = [];
    for (let index = 0; index < pending.length; index += 1) {
      const item = pending[index];
      const outcome = await sendCheckIn(supabase, {
        token: item.token,
        place: item.place,
        note: item.note,
        scanned_at: item.scanned_at,
      });
      if (outcome.kind === "offline") {
        left.push(...pending.slice(index));
        break;
      }
      if (outcome.kind === "error") {
        notes.push(`${item.member_id}: ${outcome.message}`);
        continue;
      }
      const result = outcome.result;
      if (result.status !== "ok") {
        const name = result.member?.display_name || item.member_id;
        const why =
          result.status === "duplicate"
            ? "already checked in, not recorded twice"
            : result.status === "suspicious"
              ? `used at ${result.previous?.place ?? "another place"} recently, not recorded — check with them`
              : result.reason || result.status;
        notes.push(`${name} (${formatWhen(new Date(item.scanned_at * 1000).toISOString())}): ${why}`);
      }
    }
    writeQueue(left);
    setQueued(left.length);
    if (notes.length) setAttention((current) => [...notes, ...current].slice(0, 20));
  }, []);

  useEffect(() => {
    // Browser-only state (localStorage, connectivity) is read after mount, so the server render matches.
    void Promise.resolve().then(() => {
      setPlace(readSetting(PLACE_KEY, "HINVR desk"));
      setQueued(readQueue().length);
      setOnline(navigator.onLine);
      const cached = cachedPassKeys();
      keys.current = cached.keys;
      setKeyInfo({ count: cached.keys.size, stale: !cached.fetchedAt || Date.now() - cached.fetchedAt > KEY_REFRESH_MS * 4 });
      return loadKeys().then(flush);
    });

    const supabase = createClient();
    void supabase
      .from("mandirs")
      .select("name,city")
      .eq("published", true)
      .order("sort_order")
      .then(({ data }) => {
        setPlaces((data ?? []).map((row) => [row.name, row.city].filter(Boolean).join(", ")).filter(Boolean));
      });
    void supabase
      .from("pass_checkins")
      .select("id,place,created_at,member_id,user_id,method,flag")
      .order("created_at", { ascending: false })
      .limit(12)
      .then(async ({ data }) => {
        const rows = data ?? [];
        const ids = [...new Set(rows.map((row) => row.user_id).filter(Boolean))] as string[];
        const people = ids.length ? await supabase.from("profiles").select("id,display_name").in("id", ids) : { data: [] };
        const names = new Map((people.data ?? []).map((row) => [row.id, row.display_name || "Member"]));
        setRecent(
          rows.map((row) => ({
            id: row.id,
            place: row.place,
            created_at: row.created_at,
            method: row.method,
            flag: row.flag,
            name: names.get(row.user_id) || row.member_id || "Member",
          })),
        );
      });

    function goOnline() {
      setOnline(true);
      void loadKeys().then(flush);
    }
    function goOffline() {
      setOnline(false);
    }
    window.addEventListener("online", goOnline);
    window.addEventListener("offline", goOffline);
    const keyTimer = setInterval(() => void loadKeys(), KEY_REFRESH_MS);
    const flushTimer = setInterval(() => void flush(), 60_000);
    return () => {
      window.removeEventListener("online", goOnline);
      window.removeEventListener("offline", goOffline);
      clearInterval(keyTimer);
      clearInterval(flushTimer);
    };
  }, [flush, loadKeys]);

  function addRecent(result: CheckInResult) {
    if (!result.checkin) return;
    const visit: Visit = {
      id: result.checkin.id,
      place: result.checkin.place,
      created_at: result.checkin.created_at,
      name: result.member?.display_name || "Member",
      method: result.status === "manual" ? "manual" : "signed",
      flag: result.flag,
    };
    setRecent((current) => [visit, ...current.filter((row) => row.id !== visit.id)].slice(0, 12));
  }

  async function verifyOffline(token: string) {
    const scannedAt = Math.floor(Date.now() / 1000);
    if (keys.current.size === 0) {
      show({ tone: "red", title: "Offline, no keys saved", detail: "This desk has never loaded the pass keys. Connect once, then it can verify offline.", autoClose: true });
      return;
    }
    try {
      const claims = await verifyPassToken(token, keys.current, scannedAt);
      const queue = readQueue();
      queue.push({
        id: crypto.randomUUID(),
        token,
        place: placeRef.current.trim() || "HINVR desk",
        note: noteRef.current.trim(),
        scanned_at: scannedAt,
        member_id: claims.m,
      });
      writeQueue(queue);
      setQueued(queue.length);
      setNote("");
      show({
        tone: "amber",
        title: "Verified offline",
        detail: "Signature and expiry are good. It will be recorded when the desk is back online. Revocations can't be checked offline.",
        member: { display_name: claims.m, tier: claims.t, member_id: claims.m },
        autoClose: true,
      });
    } catch (error) {
      show({ tone: "red", title: "Not valid", detail: error instanceof PassTokenError ? error.message : "That code is not a signed pass.", autoClose: true });
    }
  }

  function present(result: CheckInResult, retry: (confirm: boolean) => void) {
    const member = result.member;
    switch (result.status) {
      case "ok":
        addRecent(result);
        setNote("");
        show({
          tone: "green",
          title: result.flag ? "Checked in (confirmed)" : "Valid · checked in",
          member,
          detail: `${result.checkin?.place ?? ""}`,
          autoClose: true,
        });
        return;
      case "manual":
        if (result.recorded) {
          addRecent(result);
          setNote("");
          show({ tone: "amber", title: "Manual check-in recorded", detail: "Not a signed pass.", member, autoClose: true });
          return;
        }
        show({
          tone: "amber",
          title: "Manual — check photo ID",
          detail:
            (result.previous ? `Already checked in at ${result.previous.place}, ${formatWhen(result.previous.at)}. ` : "") +
            "Typed member ID, not a signed pass. Match the name to a government photo ID before checking in.",
          member,
          confirm: { label: "Photo ID matches — check in", run: () => retry(true) },
          autoClose: false,
        });
        return;
      case "duplicate":
        show({
          tone: "amber",
          title: "Already checked in",
          detail: result.previous ? `At ${result.previous.place}, ${formatWhen(result.previous.at)}.` : "Within the last 30 minutes.",
          member,
          confirm: { label: "Check in again", run: () => retry(true) },
          autoClose: false,
        });
        return;
      case "suspicious":
        show({
          tone: "red",
          title: "Used at another place",
          detail: `This pass was checked in at ${result.previous?.place ?? "another place"}${
            result.previous ? `, ${formatWhen(result.previous.at)}` : ""
          }. Check photo ID and ask. This is logged.`,
          member,
          confirm: { label: "Photo ID matches — check in", run: () => retry(true) },
          autoClose: false,
        });
        return;
      default:
        show({ tone: "red", title: "Not valid", detail: result.reason || "Refused.", member, autoClose: true });
    }
  }

  async function submit(body: { token?: string; member_id?: string }, confirm = false) {
    busyRef.current = true;
    setBusy(true);
    const outcome = await sendCheckIn(createClient(), {
      ...body,
      place: placeRef.current.trim() || "HINVR desk",
      note: noteRef.current.trim(),
      confirm,
    });
    busyRef.current = false;
    setBusy(false);
    if (outcome.kind === "offline") {
      setOnline(false);
      if (body.token) await verifyOffline(body.token);
      else show({ tone: "red", title: "Offline", detail: "A member ID can only be checked online. Scan their pass instead.", autoClose: true });
      return;
    }
    if (outcome.kind === "error") {
      show({ tone: "red", title: "Couldn't check in", detail: outcome.message, autoClose: true });
      return;
    }
    present(outcome.result, (again) => {
      setShown(null);
      void submit(body, again);
    });
  }

  function handleScan(raw: string, fromCamera: boolean) {
    if (busyRef.current || (shownRef.current && !shownRef.current.autoClose)) return;
    const now = Date.now();
    const seen = lastSeen.current.get(raw);
    if (fromCamera && seen && now - seen < COOLDOWN_MS) return;
    lastSeen.current.set(raw, now);
    const parsed = classifyScan(raw, fromCamera);
    if (parsed.kind === "rejected") {
      show({ tone: "red", title: "Not a pass", detail: parsed.reason, autoClose: true });
      return;
    }
    setShown(null);
    if (parsed.kind === "signed") void submit({ token: parsed.token });
    else void submit({ member_id: parsed.memberId || undefined, ...(parsed.userId ? { user_id: parsed.userId } : {}) });
  }

  useEffect(() => {
    handleRef.current = handleScan;
  });

  useEffect(() => {
    if (!cameraOn) return;
    let stopped = false;
    (async () => {
      try {
        const { Html5Qrcode, Html5QrcodeSupportedFormats } = await import("html5-qrcode");
        if (stopped) return;
        const instance = new Html5Qrcode("pass-reader", {
          formatsToSupport: [Html5QrcodeSupportedFormats.QR_CODE],
          useBarCodeDetectorIfSupported: true,
          verbose: false,
        });
        scanner.current = instance as unknown as typeof scanner.current;
        await instance.start(
          { facingMode: "environment" },
          {
            fps: 10,
            qrbox: (width: number, height: number) => {
              const size = Math.max(160, Math.floor(Math.min(width, height) * 0.75));
              return { width: size, height: size };
            },
          },
          (decoded: string) => {
            if (!stopped) handleRef.current(decoded, true);
          },
          () => {},
        );
        if (stopped) {
          await instance.stop().catch(() => {});
          return;
        }
        try {
          const feature = instance.getRunningTrackCameraCapabilities().torchFeature();
          setTorch({ supported: feature.isSupported(), on: false });
        } catch {
          setTorch({ supported: false, on: false });
        }
      } catch (error) {
        if (!stopped) {
          setCameraError(error instanceof Error ? error.message : "Camera unavailable. Type the member ID.");
          setCameraOn(false);
        }
      }
    })();
    return () => {
      stopped = true;
      const current = scanner.current;
      scanner.current = null;
      setTorch({ supported: false, on: false });
      current?.stop().catch(() => {});
    };
  }, [cameraOn]);

  async function toggleTorch() {
    try {
      const next = !torch.on;
      await scanner.current?.getRunningTrackCameraCapabilities().torchFeature().apply(next);
      setTorch({ supported: true, on: next });
    } catch {
      setTorch({ supported: false, on: false });
    }
  }

  const tone = shown ? tones[shown.tone] : null;

  return (
    <div className="rounded-2xl bg-[#1A120C] p-4 text-[#F3E9DA] sm:p-6">
      <div className="mb-4 flex flex-wrap items-center gap-2 text-xs">
        <span className={`rounded-full px-2.5 py-1 ${online ? "bg-emerald-900/60 text-emerald-200" : "bg-amber-900/60 text-amber-200"}`}>
          {online ? "Online" : "Offline — scans verify locally"}
        </span>
        {queued > 0 ? (
          <span className="rounded-full bg-amber-900/60 px-2.5 py-1 text-amber-200">
            {queued} waiting to record
          </span>
        ) : null}
        <span className={`rounded-full px-2.5 py-1 ${keyInfo.stale ? "bg-red-900/60 text-red-200" : "bg-white/10 text-[#CDBFA8]"}`}>
          {keyInfo.count === 0 ? "No pass keys yet" : keyInfo.stale ? "Pass keys out of date" : `${keyInfo.count} pass key${keyInfo.count === 1 ? "" : "s"}`}
        </span>
      </div>

      <div className="grid gap-6 lg:grid-cols-[minmax(0,460px)_minmax(0,1fr)]">
        <div className="space-y-4">
          <div className="overflow-hidden rounded-xl bg-black">
            {cameraOn ? (
              <div id="pass-reader" className="w-full" />
            ) : (
              <div className="grid h-64 place-items-center px-6 text-center text-sm text-[#CDBFA8]">
                {cameraError || "Camera is off. Open it once; it keeps scanning."}
              </div>
            )}
          </div>
          <div className="flex flex-wrap gap-2">
            <button
              type="button"
              onClick={() => {
                setCameraError("");
                setCameraOn((on) => !on);
              }}
              className="rounded-lg bg-[#D9A441] px-4 py-2.5 text-sm font-semibold text-[#1A120C]"
            >
              {cameraOn ? "Stop camera" : "Open camera"}
            </button>
            {cameraOn && torch.supported ? (
              <button
                type="button"
                aria-pressed={torch.on}
                onClick={toggleTorch}
                className="rounded-lg border border-white/20 px-4 py-2.5 text-sm"
              >
                {torch.on ? "Torch off" : "Torch on"}
              </button>
            ) : null}
          </div>
          <div className="grid gap-3 sm:grid-cols-2">
            <label className="block text-sm">
              <span className="mb-1.5 block text-[#CDBFA8]">Where (this device)</span>
              <input
                value={place}
                onChange={(event) => {
                  setPlace(event.target.value);
                  writeSetting(PLACE_KEY, event.target.value);
                }}
                list="mandir-places"
                className="w-full rounded-lg border border-white/15 bg-white/5 px-3 py-2 text-sm text-[#F3E9DA] outline-none focus:border-[#D9A441]"
              />
              <datalist id="mandir-places">
                {places.map((name) => (
                  <option key={name} value={name} />
                ))}
              </datalist>
            </label>
            <label className="block text-sm">
              <span className="mb-1.5 block text-[#CDBFA8]">Note for the next check-in</span>
              <input
                value={note}
                maxLength={500}
                onChange={(event) => setNote(event.target.value)}
                placeholder="Party of 3, buggy"
                className="w-full rounded-lg border border-white/15 bg-white/5 px-3 py-2 text-sm text-[#F3E9DA] outline-none placeholder:text-white/30 focus:border-[#D9A441]"
              />
            </label>
          </div>
          <form
            className="flex gap-2"
            onSubmit={(event) => {
              event.preventDefault();
              if (manual.trim()) handleRef.current(manual.trim(), false);
              setManual("");
            }}
          >
            <label className="sr-only" htmlFor="manual-code">
              Member ID or signed pass
            </label>
            <input
              id="manual-code"
              value={manual}
              onChange={(event) => setManual(event.target.value)}
              placeholder="Type member ID (HNV-2026-0001)"
              className="min-w-0 flex-1 rounded-lg border border-white/15 bg-white/5 px-3 py-2 text-sm text-[#F3E9DA] outline-none placeholder:text-white/30 focus:border-[#D9A441]"
            />
            <button
              type="submit"
              disabled={busy || !manual.trim()}
              className="rounded-lg border border-white/20 px-4 py-2 text-sm disabled:opacity-50"
            >
              {busy ? "Checking…" : "Look up"}
            </button>
          </form>
        </div>

        <div className="space-y-4">
          {attention.length > 0 ? (
            <div className="rounded-xl border border-amber-500/40 bg-amber-900/30 p-4 text-sm">
              <div className="flex items-center justify-between gap-2">
                <p className="font-semibold text-amber-200">Offline scans that need a look</p>
                <button type="button" className="text-xs text-amber-200 underline" onClick={() => setAttention([])}>
                  Clear
                </button>
              </div>
              <ul className="mt-2 space-y-1 text-amber-100">
                {attention.map((line, index) => (
                  <li key={`${index}-${line}`}>{line}</li>
                ))}
              </ul>
            </div>
          ) : null}
          <div className="rounded-xl bg-white/5 p-4">
            <p className="text-xs tracking-[0.16em] text-[#D9A441] uppercase">Latest check-ins</p>
            {recent.length === 0 ? (
              <p className="mt-3 text-sm text-[#CDBFA8]">None yet.</p>
            ) : (
              <ul className="mt-2 divide-y divide-white/10">
                {recent.map((visit) => (
                  <li key={visit.id} className="py-2 text-sm">
                    <span className="font-medium">{visit.name}</span>
                    <span className="text-[#CDBFA8]">
                      {" "}
                      · {visit.place || "HINVR desk"} · {formatWhen(visit.created_at)}
                      {visit.method === "manual" ? " · manual" : ""}
                      {visit.flag ? ` · ${visit.flag}` : ""}
                    </span>
                  </li>
                ))}
              </ul>
            )}
          </div>
        </div>
      </div>

      <div aria-live="assertive" aria-atomic="true">
        {shown && tone ? (
          <div
            role="alertdialog"
            aria-label={shown.title}
            className={`fixed inset-0 z-50 flex flex-col items-center justify-center px-6 text-center text-white ${tone.panel}`}
            onClick={() => (shown.autoClose ? setShown(null) : undefined)}
          >
            <div className="grid size-40 place-items-center rounded-full border-8 border-white/80 text-8xl font-bold sm:size-56 sm:text-9xl">
              {tone.mark}
            </div>
            <p className="mt-8 text-4xl font-semibold sm:text-5xl">{shown.title}</p>
            {shown.member ? (
              <div className="mt-6 space-y-1">
                <p className="text-3xl font-semibold">{shown.member.display_name || "Member"}</p>
                <p className="text-xl text-white/90">
                  {[tierLabel(shown.member.tier), shown.member.member_id, shown.member.valid_until ? `valid till ${formatDate(shown.member.valid_until)}` : ""]
                    .filter(Boolean)
                    .join(" · ")}
                </p>
              </div>
            ) : null}
            {shown.detail ? <p className="mt-5 max-w-xl text-lg text-white/90">{shown.detail}</p> : null}
            {shown.confirm ? (
              <div className="mt-8 flex flex-wrap justify-center gap-3">
                <button
                  type="button"
                  onClick={(event) => {
                    event.stopPropagation();
                    shown.confirm?.run();
                  }}
                  className="rounded-xl bg-white px-6 py-4 text-lg font-semibold text-zinc-900"
                >
                  {shown.confirm.label}
                </button>
                <button
                  type="button"
                  onClick={(event) => {
                    event.stopPropagation();
                    setShown(null);
                  }}
                  className="rounded-xl border-2 border-white px-6 py-4 text-lg font-semibold"
                >
                  Don&apos;t check in
                </button>
              </div>
            ) : (
              <p className="mt-8 text-sm text-white/70">Tap to scan the next pass</p>
            )}
          </div>
        ) : null}
      </div>
    </div>
  );
}
