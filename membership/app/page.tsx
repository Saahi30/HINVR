import { Account } from "@/components/account";
import { ClubStory } from "@/components/club";
import { Seal } from "@/components/seal";
import { PLANS } from "@/lib/plans";

// Public, server-rendered plans and pricing. The signed-in account (opened from the app)
// is a client island; crawlers and first paint get the full page without it.
export default function MembershipPage() {
  return (
    <div className="min-h-full bg-linen">
      <header className="bg-dusk px-6 pb-24 pt-8 text-cream">
        <div className="mx-auto max-w-5xl">
          <div className="flex items-center gap-3">
            <Seal />
            <p className="text-xs tracking-[0.28em] text-gold">HINVR</p>
          </div>
          <h1 className="mt-8 max-w-xl font-serif text-5xl leading-[1.05] md:text-6xl">
            How much help your family needs.
          </h1>
          <p className="mt-4 max-w-lg text-lg text-cream-muted">
            One year at the desk. Live darshan stays simple. Higher plans add the pass and a person.
          </p>
          <p className="mt-6 max-w-lg text-cream-muted">
            Request a plan in the HINVR app. The invoice appears here after the desk confirms it.
          </p>
        </div>
      </header>

      <main className="mx-auto -mt-14 max-w-5xl px-6 pb-20">
        <Account />

        <section aria-labelledby="plans-heading">
          <h2 id="plans-heading" className="sr-only">
            Plans
          </h2>
          <div className="grid items-stretch gap-4 sm:grid-cols-2 xl:grid-cols-4">
            {PLANS.map((plan) => (
              <TierCard key={plan.name} plan={plan} />
            ))}
          </div>
          <p className="mt-4 text-sm text-muted">Prices are per year, in Indian rupees, including for NRI members.</p>
        </section>

        <ClubStory />
      </main>
    </div>
  );
}

function TierCard({ plan }: { plan: (typeof PLANS)[number] }) {
  return (
    <article className="flex flex-col rounded-[28px] bg-ivory p-6 shadow-[0_16px_40px_rgba(26,18,12,0.08)]">
      <p className="text-xs tracking-[0.16em] text-gold">ONE YEAR</p>
      <h3 className="mt-4 font-serif text-3xl">{plan.name}</h3>
      <p className="mt-1 text-muted">{plan.audience}</p>
      <p className="mt-6 font-serif text-4xl text-ink">
        {plan.price}
        <span className="font-sans text-base text-muted"> / year</span>
      </p>
      <ul className="mt-6 space-y-2.5 text-ink">
        {plan.benefits.map((benefit) => (
          <li key={benefit} className="flex gap-3">
            <span className="mt-[0.55rem] h-px w-3 shrink-0 bg-gold" />
            <span className="text-muted">{benefit}</span>
          </li>
        ))}
      </ul>
    </article>
  );
}
