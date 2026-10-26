import "jsr:@supabase/functions-js/edge-runtime.d.ts";
import { getCaller, hasRole, readEnv, serviceClient, staffRole } from "../_shared/auth.ts";
import { sendPush } from "../_shared/fcm.ts";
import { json, readJson, withCors } from "../_shared/http.ts";

/**
 * Broadcast a notice to every registered phone. Owner and admin only.
 * POST { preview: true }           -> { tokens } (how many phones would get it; nothing is sent)
 * POST { title, body }             -> sends
 */
Deno.serve(withCors(async (req: Request) => {
  if (req.method !== "POST") return json({ error: "Couldn't send that notice." }, 405);

  const env = readEnv();
  if (!env) return json({ error: "Function isn't configured." }, 500);
  const caller = await getCaller(req, env);
  if (!caller) return json({ error: "Sign in again." }, 401);

  const admin = serviceClient(env);
  const role = await staffRole(admin, caller.userId);
  if (!hasRole(role, ["owner", "admin"])) return json({ error: "Only an owner or admin can send notices." }, 403);

  const payload = await readJson(req);
  if (payload.preview === true) {
    const { count, error } = await admin.from("device_tokens").select("token", { count: "exact", head: true });
    if (error) return json({ error: "Couldn't look up phones." }, 500);
    return json({ tokens: count ?? 0 });
  }

  const title = String(payload.title ?? "").trim().slice(0, 80);
  const body = String(payload.body ?? "").trim().slice(0, 400);
  if (!title || !body) return json({ error: "Write a title and a message." }, 400);

  const { data: notice, error: insertError } = await admin
    .from("notifications")
    .insert({ title, body, created_by: caller.userId })
    .select("id")
    .single();
  if (insertError || !notice?.id) return json({ error: "Couldn't save that notice." }, 500);

  const { data: tokenRows, error: tokenError } = await admin.from("device_tokens").select("token");
  if (tokenError) return json({ error: "Couldn't look up phones." }, 500);
  const tokens = (tokenRows ?? []).map((row) => String(row.token ?? "")).filter(Boolean);

  let sent = 0;
  let failed = 0;
  if (tokens.length) {
    try {
      const result = await sendPush({ tokens, title, body, notificationId: String(notice.id) });
      sent = result.sent;
      failed = result.failed;
      if (result.drop.length) {
        await admin.from("device_tokens").delete().in("token", result.drop);
      }
    } catch (error) {
      const message = error instanceof Error ? error.message : "Couldn't reach phones.";
      return json({ error: message, id: notice.id, sent: 0, failed: tokens.length }, 502);
    }
  }

  await admin.from("notifications").update({ sent_count: sent }).eq("id", notice.id);
  return json({ id: notice.id, sent, failed, tokens: tokens.length });
}));
