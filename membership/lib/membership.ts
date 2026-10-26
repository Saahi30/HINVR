// Keep tierLabel / formatInr in step with admin/lib/membership.ts.

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

/** Today in India, YYYY-MM-DD. Memberships end 23:59 IST on their last day. */
export function todayIst(now = new Date()) {
  return new Intl.DateTimeFormat("en-CA", {
    timeZone: "Asia/Kolkata",
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
  }).format(now);
}
