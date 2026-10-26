export const jsonHeaders = {
  "Content-Type": "application/json",
  Connection: "keep-alive",
};

export function json(body: Record<string, unknown>, status = 200) {
  return new Response(JSON.stringify(body), { status, headers: jsonHeaders });
}

/** Browser origins allowed to call functions, from ADMIN_ORIGINS (comma-separated, no trailing slash). */
function allowedOrigins(): Set<string> {
  return new Set(
    (Deno.env.get("ADMIN_ORIGINS") ?? "")
      .split(",")
      .map((origin) => origin.trim().replace(/\/+$/, ""))
      .filter(Boolean),
  );
}

export function corsHeaders(req: Request): Record<string, string> {
  const origin = req.headers.get("Origin") ?? "";
  const headers: Record<string, string> = { Vary: "Origin" };
  if (origin && allowedOrigins().has(origin)) {
    headers["Access-Control-Allow-Origin"] = origin;
    headers["Access-Control-Allow-Headers"] = "authorization, x-client-info, apikey, content-type";
    headers["Access-Control-Allow-Methods"] = "POST, OPTIONS";
    headers["Access-Control-Max-Age"] = "86400";
  }
  return headers;
}

/**
 * Answers CORS preflight and adds CORS headers for allowed origins. Phones and cron
 * send no Origin, so they are unaffected. Other origins get no Allow-Origin header.
 */
export function withCors(handler: (req: Request) => Promise<Response>) {
  return async (req: Request): Promise<Response> => {
    const cors = corsHeaders(req);
    if (req.method === "OPTIONS") {
      return new Response(null, { status: cors["Access-Control-Allow-Origin"] ? 204 : 403, headers: cors });
    }
    const response = await handler(req);
    const headers = new Headers(response.headers);
    for (const [name, value] of Object.entries(cors)) headers.set(name, value);
    return new Response(response.body, { status: response.status, statusText: response.statusText, headers });
  };
}

/** Reads a JSON body, or {} when it is missing or malformed. */
export async function readJson(req: Request): Promise<Record<string, unknown>> {
  try {
    const body = await req.json();
    return body && typeof body === "object" && !Array.isArray(body) ? (body as Record<string, unknown>) : {};
  } catch {
    return {};
  }
}
