"use client";

import { useEffect, useState } from "react";
import type { EmailOtpType, SupabaseClient, User } from "@supabase/supabase-js";
import { formatDate, formatInr, tierLabel, todayIst } from "@/lib/membership";
import { createClient } from "@/lib/supabase";

type Place = { label: string; address: string };

type Profile = {
  display_name: string;
  city: string;
  language_tag: string;
  audience: string;
  tier: string;
  member_id: string;
  valid_until: string | null;
  phone_e164: string;
  addresses: Place[];
};

type Purchase = {
  id: string;
  tier: string;
  member_id: string;
  amount_inr: number;
  invoice_number: string;
  purchased_on: string;
  valid_from: string;
  valid_until: string;
};

type State =
  | { status: "checking" }
  | { status: "signed-out" }
  | { status: "opening" }
  | { status: "error"; message: string }
  | { status: "ready"; user: User; profile: Profile | null; purchases: Purchase[] };

const OTP_TYPES = new Set<EmailOtpType>(["magiclink", "email", "signup"]);

/**
 * The phone opens <site>#handoff=<token_hash>&type=magiclink. The fragment never reaches a
 * server or a log. Older app builds put it in the query string; both are read, then removed.
 */
function takeHandoff(): { tokenHash: string; type: EmailOtpType } | null {
  const fromHash = new URLSearchParams(window.location.hash.replace(/^#/, ""));
  const fromQuery = new URLSearchParams(window.location.search);
  const tokenHash =
    fromHash.get("handoff") || fromHash.get("token_hash") || fromQuery.get("handoff") || fromQuery.get("token_hash") || "";
  const requested = (fromHash.get("type") || fromQuery.get("type") || "") as EmailOtpType;
  if (!tokenHash) return null;
  window.history.replaceState(null, "", window.location.pathname);
  return { tokenHash, type: OTP_TYPES.has(requested) ? requested : "magiclink" };
}

function handoffError(error: { code?: string; status?: number; name?: string; message?: string }) {
  if (error.name === "AuthRetryableFetchError" || error.status === 0 || /fetch|network/i.test(error.message ?? "")) {
    return "Couldn't reach HINVR. Check your connection, then open Manage subscription in the app again.";
  }
  if (error.code === "otp_expired" || error.status === 403) {
    return "This link has expired or was already used. Open Manage subscription in the app again.";
  }
  if (error.status === 429 || error.code === "over_request_rate_limit") {
    return "Too many tries. Wait a minute, then open Manage subscription in the app again.";
  }
  return "Couldn't open your account from this link. Open Manage subscription in the app again.";
}

async function loadAccount(supabase: SupabaseClient, user: User): Promise<State> {
  const [profileRes, purchasesRes] = await Promise.all([
    supabase
      .from("profiles")
      .select("display_name,city,language_tag,audience,tier,member_id,valid_until,phone_e164,addresses")
      .eq("id", user.id)
      .maybeSingle(),
    supabase
      .from("membership_purchases")
      .select("id,tier,member_id,amount_inr,invoice_number,purchased_on,valid_from,valid_until")
      .eq("user_id", user.id)
      .order("purchased_on", { ascending: false }),
  ]);
  if (profileRes.error || purchasesRes.error) {
    return { status: "error", message: "Couldn't load your account. Try again from the app." };
  }
  return {
    status: "ready",
    user,
    profile: (profileRes.data as Profile | null) ?? null,
    purchases: (purchasesRes.data as Purchase[] | null) ?? [],
  };
}

function languageLabel(tag: string) {
  return tag === "hi" ? "हिन्दी" : "English";
}

function audienceLabel(value: string) {
  if (value === "Parents") return "Parents in India";
  if (value === "Family") return "Whole family";
  return "Me";
}

export function Account() {
  const [state, setState] = useState<State>({ status: "checking" });

  useEffect(() => {
    let cancelled = false;
    const update = (next: State) => {
      if (!cancelled) setState(next);
    };

    async function open() {
      await Promise.resolve();
      const handoff = takeHandoff();
      let supabase: SupabaseClient;
      try {
        supabase = createClient();
      } catch {
        update(handoff ? { status: "error", message: "The membership site is not set up yet." } : { status: "signed-out" });
        return;
      }
      if (handoff) {
        update({ status: "opening" });
        const { data, error } = await supabase.auth.verifyOtp({ token_hash: handoff.tokenHash, type: handoff.type });
        if (error || !data.user) {
          update({ status: "error", message: error ? handoffError(error) : "Couldn't open your account." });
          return;
        }
        update(await loadAccount(supabase, data.user));
        return;
      }
      const { data, error } = await supabase.auth.getUser();
      if (error || !data.user) {
        update({ status: "signed-out" });
        return;
      }
      update(await loadAccount(supabase, data.user));
    }

    open().catch(() => update({ status: "error", message: "Couldn't open your account. Try again from the app." }));
    return () => {
      cancelled = true;
    };
  }, []);

  async function signOut() {
    try {
      await createClient().auth.signOut();
    } finally {
      setState({ status: "signed-out" });
    }
  }

  if (state.status === "checking" || state.status === "signed-out") return null;

  if (state.status === "opening" || state.status === "error") {
    return (
      <section
        className="mb-6 rounded-[28px] bg-ivory p-7 shadow-[0_16px_40px_rgba(26,18,12,0.06)]"
        role={state.status === "error" ? "alert" : "status"}
        aria-live="polite"
      >
        <p className="text-xs tracking-[0.18em] text-gold">YOUR ACCOUNT</p>
        <p className="mt-3 text-lg">{state.status === "opening" ? "Opening your account…" : state.message}</p>
      </section>
    );
  }

  const { user, profile, purchases } = state;
  const name = profile?.display_name?.trim() || "Member";
  const current = profile?.tier ?? "None";
  const places = profile?.addresses ?? [];

  return (
    <section aria-labelledby="account-heading" className="mb-10">
      <article className="rounded-[28px] bg-dusk p-7 text-cream shadow-[0_16px_40px_rgba(26,18,12,0.12)]">
        <div className="flex flex-wrap items-start justify-between gap-4">
          <div>
            <p className="text-xs tracking-[0.18em] text-gold">YOUR ACCOUNT</p>
            <h2 id="account-heading" className="mt-3 font-serif text-4xl">
              {name}
            </h2>
            <p className="mt-1 text-cream-muted">{user.email}</p>
          </div>
          <button
            type="button"
            onClick={signOut}
            className="rounded-full border border-cream/30 px-4 py-2 text-sm text-cream hover:border-gold"
          >
            Sign out
          </button>
        </div>
        <div className="mt-6 flex flex-wrap items-center gap-3 text-sm text-cream-muted">
          <p className="rounded-full border border-gold/40 px-3 py-1 text-xs tracking-[0.16em] text-gold">
            {current === "None" ? "NO PLAN YET" : `${tierLabel(current).toUpperCase()} · THIS YEAR`}
          </p>
          {profile?.member_id ? <p>{profile.member_id}</p> : null}
          {profile?.valid_until ? <p>Valid till {formatDate(profile.valid_until)}</p> : null}
          <p>{audienceLabel(profile?.audience || "Me")}</p>
          {profile?.city ? <p>{profile.city}</p> : null}
        </div>
      </article>

      <h3 className="mt-8 font-serif text-3xl">This year</h3>
      {purchases.length === 0 ? (
        <article className="mt-4 rounded-[28px] bg-ivory p-7 shadow-[0_16px_40px_rgba(26,18,12,0.06)]">
          <p className="text-xs tracking-[0.18em] text-gold">INVOICE</p>
          <p className="mt-3 font-serif text-3xl">{tierLabel(current)}</p>
          <p className="mt-6 border-t border-dashed border-gold/40 pt-4 text-muted">The desk has not issued an invoice yet.</p>
        </article>
      ) : (
        <div className="mt-4 grid gap-4">
          {purchases.map((purchase, index) => (
            <InvoiceCard key={purchase.id} purchase={purchase} latest={index === 0} />
          ))}
        </div>
      )}

      <div className="mt-4 grid gap-4 md:grid-cols-2">
        <article className="rounded-[28px] bg-ivory p-7 shadow-[0_16px_40px_rgba(26,18,12,0.06)]">
          <h3 className="text-xs tracking-[0.18em] text-gold">ACCOUNT</h3>
          <dl className="mt-5 grid gap-5 sm:grid-cols-2">
            <Fact label="City" value={profile?.city || "—"} />
            <Fact label="Phone" value={profile?.phone_e164 || "—"} />
            <Fact label="Language" value={languageLabel(profile?.language_tag || "en")} />
            <Fact label="For" value={audienceLabel(profile?.audience || "Me")} />
          </dl>
        </article>
        <article className="rounded-[28px] bg-ivory p-7 shadow-[0_16px_40px_rgba(26,18,12,0.06)]">
          <h3 className="text-xs tracking-[0.18em] text-gold">PLACES</h3>
          {places.length === 0 ? (
            <p className="mt-5 text-muted">No places yet.</p>
          ) : (
            <ul className="mt-5 space-y-4">
              {places.map((place) => (
                <li key={`${place.label}-${place.address}`}>
                  <p className="font-medium">{place.label}</p>
                  <p className="text-muted">{place.address}</p>
                </li>
              ))}
            </ul>
          )}
        </article>
      </div>
    </section>
  );
}

function InvoiceCard({ purchase, latest }: { purchase: Purchase; latest: boolean }) {
  const active = purchase.valid_until && purchase.valid_until.slice(0, 10) >= todayIst();
  const status = latest ? (active ? "ACTIVE" : "ENDED") : "EARLIER";
  return (
    <article className="overflow-hidden rounded-[28px] bg-ivory shadow-[0_16px_40px_rgba(26,18,12,0.06)]">
      <div className="flex flex-wrap items-end justify-between gap-4 bg-dusk px-7 py-6 text-cream">
        <div>
          <p className="text-xs tracking-[0.18em] text-gold">INVOICE {purchase.invoice_number}</p>
          <p className="mt-2 font-serif text-4xl">{tierLabel(purchase.tier)}</p>
        </div>
        <div className="text-right">
          <p className="font-serif text-3xl text-gold">{formatInr(purchase.amount_inr)}</p>
          <p className="mt-1 text-xs tracking-[0.16em] text-gold">{status}</p>
        </div>
      </div>
      <dl className="grid gap-5 px-7 py-6 sm:grid-cols-4">
        <Fact label="Bought" value={formatDate(purchase.purchased_on)} />
        <Fact label="Member ID" value={purchase.member_id || "—"} />
        <Fact label="Valid from" value={formatDate(purchase.valid_from)} />
        <Fact label="Valid until" value={formatDate(purchase.valid_until)} />
      </dl>
    </article>
  );
}

function Fact({ label, value }: { label: string; value: string }) {
  return (
    <div>
      <dt className="text-xs tracking-[0.16em] text-gold">{label.toUpperCase()}</dt>
      <dd className="mt-1 text-lg">{value}</dd>
    </div>
  );
}
