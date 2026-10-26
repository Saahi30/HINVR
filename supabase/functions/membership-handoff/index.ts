import "jsr:@supabase/functions-js/edge-runtime.d.ts";
import { getCaller, readEnv, serviceClient } from "../_shared/auth.ts";
import { json, withCors } from "../_shared/http.ts";

/**
 * Trades a fresh phone session for a one-time magic-link token the membership site
 * verifies. The phone opens <membership site>#handoff=<token_hash>&type=<type>.
 *
 * - The access token must have been minted in the last 5 minutes, so a leaked older
 *   token cannot be turned into a new long-lived web session. The phone refreshes its
 *   session right before calling.
 * - The token hash is single use (Auth consumes it on verify) and generating a new one
 *   replaces the previous link for that user. It expires with the project's email OTP
 *   expiry (set it short, see admin/README.md).
 */
const MAX_TOKEN_AGE_SECONDS = 5 * 60;

Deno.serve(withCors(async (req: Request) => {
  if (req.method !== "POST") {
    return json({ error: "Couldn't open membership." }, 405);
  }

  const env = readEnv();
  if (!env) return json({ error: "Couldn't open membership." }, 500);
  const caller = await getCaller(req, env);
  if (!caller?.email) {
    return json({ error: "Sign in again." }, 401);
  }
  if (caller.tokenAgeSeconds == null || caller.tokenAgeSeconds > MAX_TOKEN_AGE_SECONDS || caller.tokenAgeSeconds < -60) {
    return json({ error: "Sign in again.", code: "stale_session" }, 401);
  }

  const admin = serviceClient(env);
  const { data, error } = await admin.auth.admin.generateLink({
    type: "magiclink",
    email: caller.email,
  });
  const tokenHash = data?.properties?.hashed_token;
  if (error || !tokenHash) {
    const message = error?.message ?? "";
    if (message.toLowerCase().includes("rate")) {
      return json({ error: "Too many tries. Wait a minute, then try again." }, 429);
    }
    return json({ error: "Couldn't open membership." }, 500);
  }

  const type = data.properties.verification_type || "magiclink";
  return json({ token_hash: tokenHash, type });
}));
