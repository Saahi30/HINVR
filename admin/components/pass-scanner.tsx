"use client";

import { useEffect, useRef, useState } from "react";
import { Alert, Badge, Button, Card, fieldClass } from "@/components/ui";
import { createClient } from "@/lib/supabase/client";
import { formatWhen } from "@/lib/ops";
import { tierLabel } from "@/lib/membership";
import { classifyScan } from "@/lib/pass-code";
import { loadPassPublicKeys } from "@/lib/pass-keys";
import { PassTokenError, verifyPassToken } from "@/lib/pass-token";

type Member = {
  id: string;
  display_name: string;
  city: string;
  phone_e164: string;
  tier: string;
  member_id: string;
  valid_until: string;
  audience: string;
  addresses: Array<{ label: string; address: string }>;
};

type Visit = {
  id: string;
  place: string;
  note: string;
  created_at: string;
  member_id?: string;
  user_id?: string;
  display_name?: string;
};

const memberColumns =
  "id,display_name,city,phone_e164,tier,member_id,valid_until,audience,addresses";

export function PassScanner() {
  const [cameraOn, setCameraOn] = useState(false);
  const [cameraError, setCameraError] = useState("");
  const [code, setCode] = useState("");
  const [member, setMember] = useState<Member | null>(null);
  const [visits, setVisits] = useState<Visit[]>([]);
  const [recent, setRecent] = useState<Visit[]>([]);
  const [place, setPlace] = useState("HINVR desk");
  const [note, setNote] = useState("");
  const [places, setPlaces] = useState<string[]>([]);
  const [message, setMessage] = useState("");
  const [ok, setOk] = useState(false);
  const [busy, setBusy] = useState(false);
  const [proof, setProof] = useState("");
  const [credentialId, setCredentialId] = useState("");
  const scanned = useRef("");
  const scannerRef = useRef<{ stop: () => Promise<void> } | null>(null);
  const lookupRef = useRef<(raw: string, fromCamera?: boolean) => void>(() => {});
  lookupRef.current = (raw, fromCamera) => {
    void lookup(raw, fromCamera === true);
  };

  useEffect(() => {
    const supabase = createClient();
    supabase
      .from("mandirs")
      .select("name,city")
      .eq("published", true)
      .order("sort_order")
      .then(({ data }) => {
        const names = (data ?? [])
          .map((row) => [row.name, row.city].filter(Boolean).join(", "))
          .filter(Boolean);
        setPlaces(names);
      });
    loadRecent();
  }, []);

  useEffect(() => {
    if (!cameraOn) return;
    let stopped = false;
    (async () => {
      try {
        const { Html5Qrcode } = await import("html5-qrcode");
        if (stopped) return;
        const instance = new Html5Qrcode("pass-reader");
        scannerRef.current = instance;
        await instance.start(
          { facingMode: "environment" },
          { fps: 8, qrbox: { width: 220, height: 220 } },
          (decoded) => {
            if (stopped || scanned.current === decoded) return;
            scanned.current = decoded;
            setCode(decoded);
            setCameraOn(false);
            lookupRef.current(decoded, true);
          },
          () => {},
        );
        if (stopped) await instance.stop().catch(() => {});
      } catch (error) {
        if (!stopped) {
          setCameraError(error instanceof Error ? error.message : "Camera unavailable. Type the member ID.");
          setCameraOn(false);
        }
      }
    })();
    return () => {
      stopped = true;
      const current = scannerRef.current;
      scannerRef.current = null;
      current?.stop().catch(() => {});
    };
  }, [cameraOn]);

  async function loadRecent() {
    const supabase = createClient();
    const { data } = await supabase
      .from("pass_checkins")
      .select("id,place,note,created_at,member_id,user_id")
      .order("created_at", { ascending: false })
      .limit(12);
    const rows = (data ?? []) as Visit[];
    const ids = [...new Set(rows.map((row) => row.user_id).filter(Boolean))] as string[];
    let names = new Map<string, string>();
    if (ids.length) {
      const people = await supabase.from("profiles").select("id,display_name").in("id", ids);
      names = new Map((people.data ?? []).map((row) => [row.id, row.display_name || "Member"]));
    }
    setRecent(rows.map((row) => ({ ...row, display_name: names.get(row.user_id || "") || row.member_id || "Member" })));
  }

  async function lookup(raw: string, fromCamera = false) {
    const parsed = classifyScan(raw, fromCamera);
    setCredentialId("");
    setProof("");
    setVisits([]);
    if (parsed.kind === "rejected") {
      setMember(null);
      setOk(false);
      setMessage(parsed.reason);
      return;
    }
    if (parsed.kind === "signed") {
      await lookupSigned(parsed.token);
      return;
    }
    await lookupManual(parsed.memberId, parsed.userId);
  }

  async function showHistory(supabase: ReturnType<typeof createClient>, userId: string) {
    const history = await supabase
      .from("pass_checkins")
      .select("id,place,note,created_at")
      .eq("user_id", userId)
      .order("created_at", { ascending: false })
      .limit(8);
    setVisits((history.data ?? []) as Visit[]);
  }

  async function lookupManual(memberId: string, userId: string) {
    setBusy(true);
    setMessage("");
    setOk(false);
    const supabase = createClient();
    const query = supabase.from("profiles").select(memberColumns);
    const result = userId ? await query.eq("id", userId).maybeSingle() : await query.eq("member_id", memberId).maybeSingle();
    setBusy(false);
    if (result.error) {
      setMember(null);
      setMessage(result.error.message);
      return;
    }
    if (!result.data) {
      setMember(null);
      setMessage("No member on that ID.");
      return;
    }
    const found = result.data as Member;
    setMember(found);
    setOk(true);
    setProof("Manual lookup. This was not a signed pass.");
    setMessage(`${found.display_name || "Member"} · ${tierLabel(found.tier)}`);
    await showHistory(supabase, found.id);
  }

  async function lookupSigned(token: string) {
    setBusy(true);
    setMessage("");
    setOk(false);
    setMember(null);
    const supabase = createClient();
    const loaded = await loadPassPublicKeys(supabase);
    if (loaded.missing && loaded.keys.size === 0) {
      setBusy(false);
      setMessage("Apply supabase/migrations/20260928120000_pass_credentials.sql, then refresh.");
      return;
    }
    let verified;
    try {
      verified = await verifyPassToken(token, loaded.keys);
    } catch (error) {
      setBusy(false);
      setMessage(error instanceof PassTokenError ? error.message : "That code is not a signed pass.");
      return;
    }
    const [cred, result] = await Promise.all([
      supabase
        .from("pass_credentials")
        .select("id,generation,revoked_at,expires_at,member_id,tier")
        .eq("id", verified.j)
        .maybeSingle(),
      supabase.from("profiles").select(`${memberColumns},pass_generation`).eq("id", verified.u).maybeSingle(),
    ]);
    setBusy(false);
    const expires = new Date(verified.e * 1000).toLocaleTimeString("en-IN", { hour: "numeric", minute: "2-digit" });
    if (cred.error || result.error) {
      setMessage(
        `${verified.m} · signature and expiry passed (${expires}). Rotation was not confirmed, so check-in was not saved.`,
      );
      return;
    }
    if (!cred.data || cred.data.revoked_at) {
      setMessage("This code was replaced. Ask them to open the pass again.");
      return;
    }
    const generation = Number((result.data as { pass_generation?: number } | null)?.pass_generation);
    if (!result.data || Number(cred.data.generation) !== verified.g || generation !== verified.g) {
      setMessage("This code was rotated. Ask them to open the pass again.");
      return;
    }
    if (cred.data.member_id !== verified.m || result.data.member_id !== verified.m) {
      setMessage("This code does not match the account.");
      return;
    }
    const found = result.data as Member;
    setMember(found);
    setCredentialId(verified.j);
    setProof(`Signed · expires ${expires}`);
    setOk(true);
    setMessage(`${found.display_name || "Member"} · ${tierLabel(found.tier)}`);
    await showHistory(supabase, found.id);
  }

  async function checkIn() {
    if (!member) return;
    const where = place.trim() || "HINVR desk";
    setBusy(true);
    setMessage("");
    setOk(false);
    const supabase = createClient();
    const { error } = await supabase.from("pass_checkins").insert({
      user_id: member.id,
      member_id: member.member_id,
      place: where,
      note: note.trim(),
      ...(credentialId ? { credential_id: credentialId } : {}),
    });
    setBusy(false);
    if (error) {
      setMessage(error.message);
      return;
    }
    setNote("");
    setOk(true);
    setMessage(`Checked in at ${where}. It shows on their phone under Pass.`);
    const history = await supabase
      .from("pass_checkins")
      .select("id,place,note,created_at")
      .eq("user_id", member.id)
      .order("created_at", { ascending: false })
      .limit(8);
    setVisits((history.data ?? []) as Visit[]);
    await loadRecent();
  }

  const passOpen = member ? ["Gold", "Platinum", "Nri"].includes(member.tier) : false;

  return (
    <div className="grid gap-6 lg:grid-cols-[minmax(0,420px)_minmax(0,1fr)]">
      <div className="space-y-3">
        <Card className="overflow-hidden p-4">
          {cameraOn ? (
            <div id="pass-reader" className="overflow-hidden rounded-lg bg-zinc-950" />
          ) : (
            <div className="grid h-52 place-items-center rounded-lg bg-zinc-950 text-sm text-zinc-400">
              {cameraError || "Camera is off"}
            </div>
          )}
          <div className="mt-3 flex flex-wrap gap-2">
            <Button
              type="button"
              variant={cameraOn ? "secondary" : "primary"}
              onClick={() => {
                scanned.current = "";
                setCameraError("");
                setCameraOn((on) => !on);
              }}
            >
              {cameraOn ? "Stop camera" : "Open camera"}
            </Button>
          </div>
          {cameraError ? <p className="mt-2 text-sm text-zinc-500">{cameraError}</p> : null}
        </Card>
        <Card className="space-y-3 p-4">
          <label className="block text-sm">
            <span className="mb-1.5 block font-medium text-zinc-700">Member ID or signed pass</span>
            <input
              value={code}
              onChange={(event) => setCode(event.target.value)}
              placeholder="HNV-2026-0001"
              className={fieldClass}
            />
          </label>
          <Button type="button" variant="secondary" disabled={busy || !code.trim()} onClick={() => lookup(code)}>
            {busy ? "Looking…" : "Look up"}
          </Button>
        </Card>
      </div>

      <div className="space-y-3">
        {message ? <Alert tone={ok ? "ok" : "error"}>{message}</Alert> : null}
        {member ? (
          <Card className="p-5">
            <div className="flex flex-wrap items-start justify-between gap-3">
              <div>
                <p className="text-lg font-semibold">{member.display_name || "Unnamed"}</p>
                <p className="mt-1 text-sm text-zinc-500">
                  {[member.member_id, member.phone_e164, member.city].filter(Boolean).join(" · ") || "No member ID yet"}
                </p>
              </div>
              <div className="text-right">
                <Badge tone={passOpen ? "green" : "amber"}>{tierLabel(member.tier)}</Badge>
                {proof ? <p className="mt-2 max-w-48 text-xs text-zinc-500">{proof}</p> : null}
              </div>
            </div>
            <dl className="mt-4 grid gap-3 text-sm sm:grid-cols-2">
              <div>
                <dt className="text-xs text-zinc-500">Valid through</dt>
                <dd>{member.valid_until || "—"}</dd>
              </div>
              <div>
                <dt className="text-xs text-zinc-500">For</dt>
                <dd>{member.audience || "—"}</dd>
              </div>
            </dl>
            {(member.addresses ?? []).length > 0 ? (
              <ul className="mt-3 space-y-1 text-sm text-zinc-600">
                {member.addresses.map((item) => (
                  <li key={`${item.label}-${item.address}`}>
                    {item.label}: {item.address}
                  </li>
                ))}
              </ul>
            ) : null}
            {!passOpen ? (
              <p className="mt-3 text-sm text-amber-800">This account does not have a Gold, Platinum, or NRI pass.</p>
            ) : null}
            <div className="mt-4 grid gap-3 sm:grid-cols-2">
              <label className="block text-sm">
                <span className="mb-1.5 block font-medium text-zinc-700">Where</span>
                <input
                  value={place}
                  onChange={(event) => setPlace(event.target.value)}
                  list="mandir-places"
                  className={fieldClass}
                />
                <datalist id="mandir-places">
                  {places.map((name) => (
                    <option key={name} value={name} />
                  ))}
                </datalist>
              </label>
              <label className="block text-sm">
                <span className="mb-1.5 block font-medium text-zinc-700">Note</span>
                <input
                  value={note}
                  onChange={(event) => setNote(event.target.value)}
                  placeholder="Party of 3, buggy"
                  className={fieldClass}
                />
              </label>
            </div>
            <Button type="button" className="mt-4" disabled={busy} onClick={checkIn}>
              {busy ? "Saving…" : "Check in"}
            </Button>
            {visits.length > 0 ? (
              <ul className="mt-5 divide-y divide-zinc-100 border-t border-zinc-100">
                {visits.map((visit) => (
                  <li key={visit.id} className="py-2 text-sm">
                    <span className="font-medium">{visit.place || "HINVR desk"}</span>
                    <span className="text-zinc-500"> · {formatWhen(visit.created_at)}</span>
                    {visit.note ? <p className="text-zinc-500">{visit.note}</p> : null}
                  </li>
                ))}
              </ul>
            ) : null}
          </Card>
        ) : (
          <Card className="p-8 text-sm text-zinc-500">Scan a pass, or type a member ID, to see the account.</Card>
        )}
        <Card className="p-4">
          <p className="text-xs font-medium tracking-wide text-zinc-500 uppercase">Latest check-ins</p>
          {recent.length === 0 ? (
            <p className="mt-3 text-sm text-zinc-500">None yet.</p>
          ) : (
            <ul className="mt-2 divide-y divide-zinc-100">
              {recent.map((visit) => (
                <li key={visit.id} className="py-2 text-sm">
                  <span className="font-medium">{visit.display_name}</span>
                  <span className="text-zinc-500">
                    {" "}
                    · {visit.place || "HINVR desk"} · {formatWhen(visit.created_at)}
                  </span>
                </li>
              ))}
            </ul>
          )}
        </Card>
      </div>
    </div>
  );
}
