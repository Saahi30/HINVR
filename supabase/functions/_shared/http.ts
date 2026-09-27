export const jsonHeaders = {
  "Content-Type": "application/json",
  Connection: "keep-alive",
};

export function json(body: Record<string, unknown>, status = 200) {
  return new Response(JSON.stringify(body), { status, headers: jsonHeaders });
}
