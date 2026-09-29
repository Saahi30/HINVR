import "jsr:@supabase/functions-js/edge-runtime.d.ts";
import { createClient } from "jsr:@supabase/supabase-js@2";
import { sendPush } from "../_shared/fcm.ts";
import { json } from "../_shared/http.ts";

Deno.serve(async (req: Request) => {
  if (req.method !== "POST") return json({ error: "Couldn't send that notice." }, 405);

  const authHeader = req.headers.get("Authorization") ?? "";
  const url = Deno.env.get("SUPABASE_URL") ?? "";
  const anonKey = Deno.env.get("SUPABASE_ANON_KEY") ?? "";
  const serviceKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY") ?? Deno.env.get("SUPABASE_SECRET_KEY") ?? "";
  if (!url || !anonKey || !serviceKey || !authHeader.toLowerCase().startsWith("bearer ")) {
    return json({ error: "Sign in again." }, 401);
  }

  let title = "";
  let body = "";
  try {
    const payload = await req.json();
    title = String(payload?.title ?? "").trim().slice(0, 80);
    body = String(payload?.body ?? "").trim().slice(0, 400);
  } catch {
    return json({ error: "Write a title and a message." }, 400);
  }
  if (!title || !body) return json({ error: "Write a title and a message." }, 400);

  const userClient = createClient(url, anonKey, {
    global: { headers: { Authorization: authHeader } },
    auth: { persistSession: false, autoRefreshToken: false },
  });
  const { data: userData, error: userError } = await userClient.auth.getUser();
  const userId = userData.user?.id;
  if (userError || !userId) return json({ error: "Sign in again." }, 401);

  const admin = createClient(url, serviceKey, {
    auth: { persistSession: false, autoRefreshToken: false },
  });
  const { data: staff } = await admin.from("staff").select("user_id").eq("user_id", userId).maybeSingle();
  if (!staff) return json({ error: "No access." }, 403);

  const { data: notice, error: insertError } = await admin
    .from("notifications")
    .insert({ title, body, created_by: userId })
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
});
