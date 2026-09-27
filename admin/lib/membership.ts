export function tierLabel(tier: string) {
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

export function nextSerial(existing: string[], prefix: string, year = new Date().getFullYear()) {
  const pattern = new RegExp(`^${prefix}-${year}-(\\d+)$`);
  let max = 0;
  for (const value of existing) {
    const match = value.match(pattern);
    if (match) max = Math.max(max, Number(match[1]));
  }
  return `${prefix}-${year}-${String(max + 1).padStart(4, "0")}`;
}

function isoDate(date: Date) {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");
  return `${year}-${month}-${day}`;
}

export function membershipWindow(from = new Date()) {
  const start = new Date(from.getFullYear(), from.getMonth(), from.getDate());
  const end = new Date(start);
  end.setFullYear(end.getFullYear() + 1);
  return {
    purchasedOn: isoDate(start),
    validFrom: isoDate(start),
    validUntilIso: isoDate(end),
    validUntilLabel: end.toLocaleDateString("en-GB", { day: "numeric", month: "short", year: "numeric" }),
  };
}
