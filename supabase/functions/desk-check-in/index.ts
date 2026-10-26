import "jsr:@supabase/functions-js/edge-runtime.d.ts";
import { getCaller, hasRole, readEnv, serviceClient, staffRole } from "../_shared/auth.ts";
import { json, readJson, withCors } from "../_shared/http.ts";
import { PassTokenError, verifyPassToken } from "../_shared/pass-token.ts";
import { verifyKeys } from "../_shared/signing-keys.ts";

/**
 * Server-side check-in for the desk scanner.
 *
 * POST { token, place, note?, confirm?, scanned_at? }     signed pass (QR)
 * POST { member_id, place, note?, confirm? }              manual lookup; never shows as a verified pass
 *
 * Always 200 with { status } for a desk decision:
 *   ok          signed pass verified and recorded
 *   manual      member found by ID; recorded only with confirm (after the desk checks photo ID)
 *   duplicate   same pass checked in within 30 minutes; recorded only with confirm
 *   suspicious  same pass used at another place within 2 hours; recorded only with confirm
 *   invalid     refused, with a reason
 * scanned_at (unix seconds) is for scans the desk verified offline and is sending later.
 */

const DESK_ROLES = ["owner", "admin", "desk"] as const;
const UUID = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

function text(value: unknown, max: number) {
  return typeof value === "string" ? value.trim().slice(0, max) : "";
}

Deno.serve(withCors(async (req: Request) => {
  if (req.method !== "POST") return json({ error: "Use POST." }, 405);
  const env = readEnv();
  if (!env) return json({ error: "Function isn't configured." }, 500);
  const caller = await getCaller(req, env);
  if (!caller) return json({ error: "Sign in again." }, 401);

  const admin = serviceClient(env);
  const role = await staffRole(admin, caller.userId);
  if (!hasRole(role, DESK_ROLES)) return json({ error: "Only owner, admin, or desk staff can check members in." }, 403);

  const body = await readJson(req);
  const token = text(body.token, 4096);
  const memberId = text(body.member_id, 40);
  const userId = text(body.user_id, 36);
  const place = text(body.place, 200) || "HINVR desk";
  const note = text(body.note, 500);
  const confirm = body.confirm === true;
  const nowSeconds = Math.floor(Date.now() / 1000);
  let scannedAt: number | null = null;
  if (body.scanned_at != null) {
    const value = Number(body.scanned_at);
    if (!Number.isFinite(value)) return json({ error: "Bad scan time." }, 400);
    scannedAt = Math.floor(value);
    if (scannedAt > nowSeconds + 120) return json({ status: "invalid", reason: "The desk clock is ahead. Fix the time and scan again." });
  }

  let args: Record<string, unknown>;
  if (token) {
    let claims;
    try {
      const keys = await verifyKeys(admin);
      claims = await verifyPassToken(token, keys, scannedAt ?? nowSeconds);
    } catch (error) {
      const reason = error instanceof PassTokenError ? error.message : "That code is not a signed pass.";
      return json({ status: "invalid", reason });
    }
    args = {
      p_method: "signed",
      p_user_id: claims.u,
      p_member_id: claims.m,
      p_credential_id: claims.j,
      p_generation: claims.g,
    };
  } else if (memberId || UUID.test(userId)) {
    args = {
      p_method: "manual",
      p_user_id: UUID.test(userId) ? userId : null,
      p_member_id: memberId || null,
      p_credential_id: null,
      p_generation: null,
    };
  } else {
    return json({ error: "Scan a pass or type a member ID." }, 400);
  }

  const { data, error } = await admin.rpc("desk_check_in", {
    p_staff_id: caller.userId,
    ...args,
    p_place: place,
    p_note: note,
    p_confirm: confirm,
    p_scanned_at: scannedAt == null ? null : new Date(scannedAt * 1000).toISOString(),
  });
  if (error || !data) {
    if (error?.code === "42501") return json({ error: error.message }, 403);
    console.error("desk_check_in", error?.message);
    return json({ error: "Couldn't record this check-in." }, 500);
  }
  return json(data as Record<string, unknown>);
}));
