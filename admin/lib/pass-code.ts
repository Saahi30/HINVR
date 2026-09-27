const UUID = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

export type ScanCode =
  | { kind: "signed"; token: string }
  | { kind: "manual"; memberId: string; userId: string }
  | { kind: "rejected"; reason: string };

export function classifyScan(raw: string, fromCamera = false): ScanCode {
  const text = raw.trim();
  if (!text) return { kind: "rejected", reason: "That code is empty." };
  if (text.startsWith("HNV|") || text.includes("|")) {
    return {
      kind: "rejected",
      reason: "That is an unsigned string. Anyone could write it. Ask them to open the pass in the app.",
    };
  }
  if (text.startsWith("HNV1.")) return { kind: "signed", token: text };
  if (fromCamera) {
    return {
      kind: "rejected",
      reason: "That QR is not a signed pass. Ask them to open the pass in the app.",
    };
  }
  if (UUID.test(text)) return { kind: "manual", memberId: "", userId: text };
  return { kind: "manual", memberId: text, userId: "" };
}
