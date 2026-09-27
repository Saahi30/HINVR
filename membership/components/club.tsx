const ROWS: { label: string; cells: [string, string, string, string] }[] = [
  { label: "Official live darshan", cells: ["Open", "Open", "Open", "Open"] },
  { label: "Mandir directory", cells: ["Open", "Open", "Open", "Open"] },
  { label: "Concierge", cells: ["Requests", "Priority chat", "Phone", "Phone"] },
  { label: "Digital QR pass", cells: ["—", "Included", "Included", "Included"] },
  { label: "Visit and accessibility", cells: ["—", "Included", "Included", "Included"] },
  { label: "Recorded VR darshan", cells: ["—", "Included", "Included", "Included"] },
  { label: "Family on the profile", cells: ["—", "—", "Included", "Included"] },
  { label: "Physical card request", cells: ["—", "—", "Included", "Included"] },
  { label: "Partner assist", cells: ["—", "—", "Priority", "Priority"] },
];

const STEPS = [
  {
    n: "01",
    title: "The account",
    body: "Name, city, phone, and places live in the app. This page reads that same account.",
  },
  {
    n: "02",
    title: "The year",
    body: "Request a plan in the app. The desk confirms the year: member ID, dates, amount, and an invoice number.",
  },
  {
    n: "03",
    title: "The pass",
    body: "Gold and above open the QR pass and the visit desk in the app. The invoice stays here.",
  },
];

const QUESTIONS = [
  {
    q: "Is live darshan only for members?",
    a: "No. Official temple streams and the mandir directory stay open. Membership adds the pass, recorded VR, and a person at the desk.",
  },
  {
    q: "When does the pass appear?",
    a: "After Gold, Platinum, or NRI is on the account. Open Pass in the app. Darshan does not include the QR.",
  },
  {
    q: "Where is the invoice?",
    a: "On this page, after the desk confirms the request you sent from the app.",
  },
  {
    q: "What is partner assist?",
    a: "Official entry and help at a partner mandir: access, a buggy, a wheelchair, a host. It depends on that mandir. It is not an unofficial queue-jump.",
  },
];

export function ClubStory() {
  return (
    <>
      <section className="mt-16">
        <p className="text-xs tracking-[0.18em] text-gold">WHAT OPENS</p>
        <h2 className="mt-2 font-serif text-3xl">Same temples. Different desk.</h2>
        <div className="mt-5 overflow-x-auto rounded-[28px] bg-ivory shadow-[0_16px_40px_rgba(26,18,12,0.06)]">
          <table className="w-full min-w-[760px] text-left text-sm">
            <thead>
              <tr className="border-b border-gold/20 text-xs tracking-[0.16em] text-gold">
                <th className="px-6 py-4 font-medium"> </th>
                <th className="px-4 py-4 font-medium">Darshan</th>
                <th className="px-4 py-4 font-medium">Gold</th>
                <th className="px-4 py-4 font-medium">Platinum</th>
                <th className="px-6 py-4 font-medium">NRI</th>
              </tr>
            </thead>
            <tbody>
              {ROWS.map((row) => (
                <tr key={row.label} className="border-t border-black/5">
                  <th className="px-6 py-3.5 text-left font-medium text-ink">{row.label}</th>
                  {row.cells.map((cell, index) => (
                    <td
                      key={`${row.label}-${index}`}
                      className={`px-4 py-3.5 ${cell === "—" ? "text-muted/50" : "text-ink"} ${index === row.cells.length - 1 ? "pr-6" : ""}`}
                    >
                      {cell}
                    </td>
                  ))}
                </tr>
              ))}
            </tbody>
          </table>
        </div>
        <p className="mt-4 max-w-2xl text-sm text-muted">
          Directory and official live darshan stay open without a plan. Partner help depends on the mandir and on confirmed services.
        </p>
      </section>

      <section className="mt-16">
        <p className="text-xs tracking-[0.18em] text-gold">THE YEAR</p>
        <h2 className="mt-2 font-serif text-3xl">How a membership is issued.</h2>
        <ol className="mt-5 grid gap-4 md:grid-cols-3">
          {STEPS.map((step) => (
            <li key={step.n} className="rounded-[28px] bg-ivory p-6 shadow-[0_16px_40px_rgba(26,18,12,0.06)]">
              <p className="font-serif text-2xl text-gold">{step.n}</p>
              <h3 className="mt-3 font-serif text-2xl">{step.title}</h3>
              <p className="mt-2 text-muted">{step.body}</p>
            </li>
          ))}
        </ol>
      </section>

      <section className="mt-16">
        <p className="text-xs tracking-[0.18em] text-gold">QUESTIONS</p>
        <h2 className="mt-2 font-serif text-3xl">Before you ask the desk.</h2>
        <div className="mt-5 grid gap-4 md:grid-cols-2">
          {QUESTIONS.map((item) => (
            <article key={item.q} className="rounded-[28px] bg-ivory p-6 shadow-[0_16px_40px_rgba(26,18,12,0.06)]">
              <h3 className="font-serif text-2xl">{item.q}</h3>
              <p className="mt-2 text-muted">{item.a}</p>
            </article>
          ))}
        </div>
      </section>
    </>
  );
}
