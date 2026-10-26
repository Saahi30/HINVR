import { FunctionsFetchError, FunctionsHttpError, FunctionsRelayError, type SupabaseClient } from "@supabase/supabase-js";
import type { CheckInResult } from "@/lib/types";

export type CheckInRequest = {
  token?: string;
  member_id?: string;
  user_id?: string;
  place: string;
  note?: string;
  confirm?: boolean;
  /** unix seconds, for scans verified offline and sent later */
  scanned_at?: number;
};

export type CheckInOutcome =
  | { kind: "answer"; result: CheckInResult }
  | { kind: "offline" }
  | { kind: "error"; message: string };

/** One round trip: the server verifies, applies replay rules, and records. */
export async function sendCheckIn(supabase: SupabaseClient, body: CheckInRequest): Promise<CheckInOutcome> {
  if (typeof navigator !== "undefined" && navigator.onLine === false) return { kind: "offline" };
  try {
    const { data, error } = await supabase.functions.invoke("desk-check-in", { body });
    if (error) {
      if (error instanceof FunctionsFetchError || error instanceof FunctionsRelayError) return { kind: "offline" };
      if (error instanceof FunctionsHttpError) {
        const payload = await error.context.json().catch(() => null);
        return { kind: "error", message: typeof payload?.error === "string" ? payload.error : "Couldn't check in." };
      }
      return { kind: "error", message: error.message };
    }
    return { kind: "answer", result: data as CheckInResult };
  } catch {
    return { kind: "offline" };
  }
}

export type QueuedScan = {
  id: string;
  token: string;
  place: string;
  note: string;
  scanned_at: number;
  member_id: string;
};

const QUEUE_KEY = "hinvr.scan.queue";

export function readQueue(): QueuedScan[] {
  try {
    const parsed = JSON.parse(window.localStorage.getItem(QUEUE_KEY) || "[]") as QueuedScan[];
    return Array.isArray(parsed) ? parsed.filter((row) => row && row.id && row.token) : [];
  } catch {
    return [];
  }
}

export function writeQueue(rows: QueuedScan[]) {
  try {
    window.localStorage.setItem(QUEUE_KEY, JSON.stringify(rows.slice(-200)));
  } catch {
    // Storage blocked: the queue lives only in this tab.
  }
}

export function readSetting(key: string, fallback: string) {
  try {
    return window.localStorage.getItem(key) ?? fallback;
  } catch {
    return fallback;
  }
}

export function writeSetting(key: string, value: string) {
  try {
    window.localStorage.setItem(key, value);
  } catch {
    // Not persisted; fine for this session.
  }
}
