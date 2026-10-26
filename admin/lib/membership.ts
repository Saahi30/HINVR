// Keep tierLabel / formatInr in step with membership/lib/membership.ts.

export function tierLabel(tier: string | null | undefined) {
  if (tier === "Nri") return "NRI";
  if (!tier || tier === "None") return "No plan";
  return tier;
}

export function formatInr(amount: number) {
  return new Intl.NumberFormat("en-IN", {
    style: "currency",
    currency: "INR",
    maximumFractionDigits: 0,
  }).format(amount);
}

/** "28 Sep 2027" for a YYYY-MM-DD date, or "—". */
export function formatDate(value: string | null | undefined) {
  if (!value) return "—";
  const date = new Date(`${value.slice(0, 10)}T00:00:00Z`);
  if (Number.isNaN(date.getTime())) return value;
  return date.toLocaleDateString("en-GB", { day: "numeric", month: "short", year: "numeric", timeZone: "UTC" });
}

/** Today in India, YYYY-MM-DD. */
export function todayIst(now = new Date()) {
  return new Intl.DateTimeFormat("en-CA", {
    timeZone: "Asia/Kolkata",
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
  }).format(now);
}

/** Same rule as approve_membership_request: one calendar year from the start date. */
export function oneYearFrom(isoDate: string) {
  const match = /^(\d{4})-(\d{2})-(\d{2})$/.exec(isoDate);
  if (!match) return "";
  const date = new Date(Date.UTC(Number(match[1]) + 1, Number(match[2]) - 1, Number(match[3])));
  return date.toISOString().slice(0, 10);
}

/** True while the membership has not passed 23:59:59 IST on valid_until. */
export function isActiveMembership(validUntil: string | null | undefined, now = new Date()) {
  if (!validUntil) return false;
  return validUntil.slice(0, 10) >= todayIst(now);
}
