"use client";

import { useEffect, useState } from "react";
import type { EmailOtpType, User } from "@supabase/supabase-js";
import { ClubStory } from "@/components/club";
import { createClient } from "@/lib/supabase";
import { PLANS } from "@/lib/plans";

type Place = { label: string; address: string };

type Profile = {
  display_name: string;
  city: string;
  language_tag: string;
  audience: string;
  tier: string;
  member_id: string;
  valid_until: string;
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

const OTP_TYPES = new Set<EmailOtpType>(["magiclink", "email"]);

function languageLabel(tag: string) {
  if (tag === "hi") return "हिन्दी";
  return "English";
}

function audienceLabel(value: string) {
  if (value === "Parents") return "Parents in India";
  if (value === "Family") return "Whole family";
  return "Me";
}

function tierLabel(tier: string) {
  if (!tier || tier === "None") return "No membership yet";
  if (tier === "Nri") return "NRI";
  return tier;
}

function formatDate(value: string) {
  if (!value) return "—";
  const date = new Date(`${value.slice(0, 10)}T00:00:00`);
  if (Number.isNaN(date.getTime())) return value;
  return date.toLocaleDateString("en-IN", { day: "numeric", month: "short", year: "numeric" });
}

function formatRupees(amount: number) {
  return new Intl.NumberFormat("en-IN", {
    style: "currency",
    currency: "INR",
    maximumFractionDigits: 0,
  }).format(amount);
}

function purchaseStatus(validUntil: string) {
  const end = new Date(`${validUntil.slice(0, 10)}T00:00:00`);
  if (Number.isNaN(end.getTime())) return "Issued";
  const today = new Date();
  today.setHours(0, 0, 0, 0);
  return end >= today ? "Active" : "Ended";
}

export default function MembershipPage() {
  const [status, setStatus] = useState<"opening" | "ready" | "signed-out" | "error">("opening");
  const [message, setMessage] = useState("Opening your account…");
  const [user, setUser] = useState<User | null>(null);
  const [profile, setProfile] = useState<Profile | null>(null);
  const [purchases, setPurchases] = useState<Purchase[]>([]);

  useEffect(() => {
    const supabase = createClient();
    let cancelled = false;

    async function openAccount() {
      const params = new URLSearchParams(window.location.search);
      const handoff = params.get("handoff");
      const requested = params.get("type");
      const type: EmailOtpType = OTP_TYPES.has(requested as EmailOtpType)
        ? (requested as EmailOtpType)
        : "magiclink";

      if (handoff) {
        const { data, error } = await supabase.auth.verifyOtp({ token_hash: handoff, type });
        window.history.replaceState({}, "", window.location.pathname);
        if (cancelled) return;
        if (error || !data.user) {
          setStatus("error");
          setMessage("This link has already been used. Open Manage subscription in the app again.");
          return;
        }
        setUser(data.user);
      } else {
        const { data } = await supabase.auth.getUser();
        if (cancelled) return;
        if (!data.user) {
          setStatus("signed-out");
          return;
        }
        setUser(data.user);
      }

      const [{ data: row }, { data: invoices }] = await Promise.all([
        supabase
          .from("profiles")
          .select("display_name,city,language_tag,audience,tier,member_id,valid_until,phone_e164,addresses")
          .maybeSingle(),
        supabase
          .from("membership_purchases")
          .select("id,tier,member_id,amount_inr,invoice_number,purchased_on,valid_from,valid_until")
          .order("purchased_on", { ascending: false }),
      ]);
      if (cancelled) return;
      setProfile((row as Profile | null) ?? null);
      setPurchases((invoices as Purchase[] | null) ?? []);
      setStatus("ready");
    }

    openAccount().catch(() => {
      if (cancelled) return;
      setStatus("error");
      setMessage("Couldn't open your account. Try again from the app.");
    });

    return () => {
      cancelled = true;
    };
  }, []);

  if (status === "opening" || status === "error") {
    return (
      <main className="flex min-h-full flex-col justify-center bg-dusk px-6 py-20 text-cream">
        <Seal />
        <h1 className="mt-6 font-serif text-5xl">Membership</h1>
        <p className="mt-4 max-w-md text-lg text-cream-muted">{message}</p>
      </main>
    );
  }

  const signedIn = status === "ready" && user;
  const name = profile?.display_name?.trim() || "Member";
  const places = profile?.addresses ?? [];
  const current = profile?.tier ?? "None";

  return (
    <div className="min-h-full bg-linen">
      <header className="bg-dusk px-6 pb-24 pt-8 text-cream">
        <div className="mx-auto max-w-5xl">
          <div className="flex items-center gap-3">
            <Seal />
            <p className="text-xs tracking-[0.28em] text-gold">HINVR</p>
          </div>
          <h1 className="mt-8 max-w-xl font-serif text-5xl leading-[1.05] md:text-6xl">
            {signedIn ? name : "How much help your family needs."}
          </h1>
          <p className="mt-4 max-w-lg text-lg text-cream-muted">
            {signedIn
              ? user.email
              : "One year at the desk. Live darshan stays simple. Higher plans add the pass and a person."}
          </p>
          {signedIn ? (
            <div className="mt-6 flex flex-wrap items-center gap-3 text-sm text-cream-muted">
              {current !== "None" ? (
                <p className="rounded-full border border-gold/40 px-3 py-1 text-xs tracking-[0.16em] text-gold">
                  {tierLabel(current).toUpperCase()} · THIS YEAR
                </p>
              ) : (
                <p className="rounded-full border border-cream/20 px-3 py-1 text-xs tracking-[0.16em]">
                  NO PLAN YET
                </p>
              )}
              {profile?.member_id ? <p>{profile.member_id}</p> : null}
              {profile?.valid_until ? <p>Valid {profile.valid_until}</p> : null}
              <p>{audienceLabel(profile?.audience || "Me")}</p>
              {profile?.city ? <p>{profile.city}</p> : null}
            </div>
          ) : null}
          {!signedIn ? (
            <p className="mt-6 max-w-lg text-cream-muted">
              Open the HINVR app and choose Manage subscription. Your invoice opens here.
            </p>
          ) : null}
        </div>
      </header>

      <main className="mx-auto -mt-14 max-w-5xl px-6 pb-20">
        <section>
          <div className="grid items-stretch gap-4 md:grid-cols-3">
            {PLANS.map((plan) => (
              <TierCard key={plan.name} plan={plan} current={signedIn ? current : ""} />
            ))}
          </div>
        </section>

        <ClubStory />

        {signedIn ? (
          <>
            <section className="mt-14">
              <h2 className="font-serif text-3xl">This year</h2>
              {purchases.length === 0 ? (
                <article className="mt-4 rounded-[28px] bg-ivory p-7 shadow-[0_16px_40px_rgba(26,18,12,0.06)]">
                  <p className="text-xs tracking-[0.18em] text-gold">INVOICE</p>
                  <p className="mt-3 font-serif text-3xl">{tierLabel(current)}</p>
                  {profile?.member_id ? (
                    <p className="mt-2 text-muted">
                      {[profile.member_id, profile.valid_until].filter(Boolean).join(" · ")}
                    </p>
                  ) : (
                    <p className="mt-2 text-muted">No membership yet.</p>
                  )}
                  <p className="mt-6 border-t border-dashed border-gold/40 pt-4 text-muted">
                    The desk has not issued an invoice yet.
                  </p>
                </article>
              ) : (
                <div className="mt-4 grid gap-4">
                  {purchases.map((purchase, index) => (
                    <InvoiceCard key={purchase.id} purchase={purchase} latest={index === 0} />
                  ))}
                </div>
              )}
            </section>

            <section className="mt-4 grid gap-4 md:grid-cols-2">
              <article className="rounded-[28px] bg-ivory p-7 shadow-[0_16px_40px_rgba(26,18,12,0.06)]">
                <h2 className="text-xs tracking-[0.18em] text-gold">ACCOUNT</h2>
                <dl className="mt-5 grid gap-5 sm:grid-cols-2">
                  <Fact label="City" value={profile?.city || "—"} />
                  <Fact label="Phone" value={profile?.phone_e164 || "—"} />
                  <Fact label="Language" value={languageLabel(profile?.language_tag || "en")} />
                  <Fact label="For" value={audienceLabel(profile?.audience || "Me")} />
                </dl>
              </article>
              <article className="rounded-[28px] bg-ivory p-7 shadow-[0_16px_40px_rgba(26,18,12,0.06)]">
                <h2 className="text-xs tracking-[0.18em] text-gold">PLACES</h2>
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
            </section>
          </>
        ) : null}
      </main>
    </div>
  );
}

function Seal() {
  return (
    <div className="grid size-11 place-items-center rounded-full border border-gold/70 font-serif text-xl text-gold">
      H
    </div>
  );
}

function TierCard({
  plan,
  current,
}: {
  plan: (typeof PLANS)[number];
  current: string;
}) {
  const active = current === plan.name;
  const recommended = "recommended" in plan && plan.recommended;
  const featured = active || recommended;
  return (
    <article
      className={`flex flex-col rounded-[28px] p-6 shadow-[0_16px_40px_rgba(26,18,12,0.08)] ${
        active
          ? "bg-dusk text-cream ring-1 ring-gold/50 md:-mt-3"
          : recommended
            ? "bg-ivory ring-1 ring-gold/50 md:-mt-3"
            : "bg-ivory"
      }`}
    >
      <div className="flex flex-wrap gap-2">
        {recommended ? <Chip>Most popular</Chip> : null}
        {active ? <Chip>Current</Chip> : null}
        {!featured ? <p className="text-xs tracking-[0.16em] text-gold">ONE YEAR</p> : null}
      </div>
      <h3 className="mt-4 font-serif text-3xl">{plan.name}</h3>
      <p className={`mt-1 ${active ? "text-cream-muted" : "text-muted"}`}>{plan.audience}</p>
      <p className={`mt-6 font-serif text-4xl ${active ? "text-gold" : "text-ink"}`}>
        {plan.price}
        <span className={`font-sans text-base ${active ? "text-cream-muted" : "text-muted"}`}> / year</span>
      </p>
      <ul className={`mt-6 space-y-2.5 ${active ? "text-cream" : "text-ink"}`}>
        {plan.benefits.map((benefit) => (
          <li key={benefit} className="flex gap-3">
            <span className="mt-[0.55rem] h-px w-3 shrink-0 bg-gold" />
            <span className={active ? "text-cream-muted" : "text-muted"}>{benefit}</span>
          </li>
        ))}
      </ul>
    </article>
  );
}

function Chip({ children }: { children: string }) {
  return (
    <span className="rounded-full bg-gold px-2.5 py-1 text-[11px] tracking-[0.12em] text-dusk uppercase">
      {children}
    </span>
  );
}

function InvoiceCard({ purchase, latest }: { purchase: Purchase; latest: boolean }) {
  const status = purchaseStatus(purchase.valid_until);
  return (
    <article className="overflow-hidden rounded-[28px] bg-ivory shadow-[0_16px_40px_rgba(26,18,12,0.06)]">
      <div className="flex flex-wrap items-end justify-between gap-4 bg-dusk px-7 py-6 text-cream">
        <div>
          <p className="text-xs tracking-[0.18em] text-gold">INVOICE {purchase.invoice_number}</p>
          <p className="mt-2 font-serif text-4xl">{tierLabel(purchase.tier)}</p>
        </div>
        <div className="text-right">
          <p className="font-serif text-3xl text-gold">{formatRupees(purchase.amount_inr)}</p>
          <p className="mt-1 text-xs tracking-[0.16em] text-gold">{latest ? status.toUpperCase() : "EARLIER"}</p>
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

