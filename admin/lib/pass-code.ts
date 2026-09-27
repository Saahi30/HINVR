const UUID = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

export function parsePassCode(raw: string) {
  const text = raw.trim();
  if (text.startsWith("HNV|")) {
    const parts = text.split("|").map((part) => part.trim());
    const userId = parts.find((part, index) => index > 0 && UUID.test(part)) ?? "";
    return { memberId: parts[1] ?? "", userId };
  }
  if (UUID.test(text)) return { memberId: "", userId: text };
  return { memberId: text, userId: "" };
}
