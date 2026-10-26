// Aarti start times, India time, as the database stores them: 'HH:MM', 24-hour, at most 12.
export const MAX_AARTI_TIMES = 12;

const TIME = /^([01]?[0-9]|2[0-3]):([0-5][0-9])$/;

/** "6:00, 19:30" → ["06:00", "19:30"], sorted and without repeats. Blank means none (null). */
export function parseAartiTimes(text: string): { times: string[] | null; error: string } {
  const parts = text
    .split(/[,\s]+/)
    .map((part) => part.trim())
    .filter(Boolean);
  if (parts.length === 0) return { times: null, error: "" };
  const times = new Set<string>();
  for (const part of parts) {
    const match = TIME.exec(part);
    if (!match) return { times: null, error: `“${part}” is not a 24-hour time like 06:00 or 19:30.` };
    times.add(`${match[1].padStart(2, "0")}:${match[2]}`);
  }
  if (times.size > MAX_AARTI_TIMES) {
    return { times: null, error: `At most ${MAX_AARTI_TIMES} aarti times.` };
  }
  return { times: [...times].sort(), error: "" };
}

export function formatAartiTimes(times: string[] | null | undefined): string {
  return (times ?? []).join(", ");
}
