import type { InsightsDay } from "./types.ts";

/** The periods the Insights page offers, in days. */
export const INSIGHT_PERIODS = [7, 30, 90] as const;
export type InsightPeriod = (typeof INSIGHT_PERIODS)[number];

/** ?days= from the URL, falling back to 30. */
export function periodFrom(value: string | string[] | undefined): InsightPeriod {
  const n = Number(Array.isArray(value) ? value[0] : value);
  return (INSIGHT_PERIODS as readonly number[]).includes(n) ? (n as InsightPeriod) : 30;
}

const IST_OFFSET_MS = 330 * 60_000;

/** The India date (YYYY-MM-DD) of an instant. */
export function istDate(iso: string): string {
  return new Date(Date.parse(iso) + IST_OFFSET_MS).toISOString().slice(0, 10);
}

/** Every day from [since] for [days] days, with zeros where nothing was offered. */
export function fillDays(daily: InsightsDay[], since: string, days: number): InsightsDay[] {
  const byDay = new Map(daily.map((row) => [row.day, row]));
  const start = Date.parse(`${istDate(since)}T00:00:00Z`);
  return Array.from({ length: days }, (_, i) => {
    const day = new Date(start + i * 86_400_000).toISOString().slice(0, 10);
    return byDay.get(day) ?? { day, total: 0, kinds: {} };
  });
}

/** Share of [part] in [whole] as a whole percent; 0 when there is nothing to share. */
export function percent(part: number, whole: number): number {
  return whole > 0 ? Math.round((part / whole) * 100) : 0;
}

/** Entries of a count map, largest first. */
export function ranked(counts: Record<string, number> | null | undefined): Array<[string, number]> {
  return Object.entries(counts ?? {}).sort((a, b) => b[1] - a[1] || a[0].localeCompare(b[0]));
}

export const OFFERING_LABELS: Record<string, string> = {
  ghanta: "Ghanta",
  diya: "Diya",
  aarti: "Aarti",
  pushp: "Flowers",
  agarbatti: "Agarbatti",
  prasad: "Naivedya",
  tilak: "Tilak",
  daan: "Daan",
  japa: "Japa round",
  abhishek: "Abhishek",
  deepmala: "Deepmala",
};

export const DEDICATION_LABELS: Record<string, string> = {
  self: "Themselves",
  family: "Family",
  parents: "Parents",
  children: "Children",
  ancestors: "Ancestors",
  friend: "A friend",
  teacher: "A teacher",
};

export const HANDOFF_LABELS: Record<string, string> = {
  live: "Live darshan",
  tour: "360 tour",
  sanctum: "Sanctum",
};
