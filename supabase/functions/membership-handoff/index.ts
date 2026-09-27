import "jsr:@supabase/functions-js/edge-runtime.d.ts";
import { createClient } from "jsr:@supabase/supabase-js@2";

const jsonHeaders = {
  "Content-Type": "application/json",
  Connection: "keep-alive",
};

function json(body: Record<string, string>, status = 200) {
  return new Response(JSON.stringify(body), { status, headers: jsonHeaders });
}

Deno.serve(async (req: Request) => {
  if (req.method !== "POST") {
    return json({ error: "Couldn't open membership." }, 405);
  }

  const authHeader = req.headers.get("Authorization") ?? "";
  const url = Deno.env.get("SUPABASE_URL") ?? "";
  const anonKey = Deno.env.get("SUPABASE_ANON_KEY") ?? "";
  const serviceKey =
    Deno.env.get("SUPABASE_SERVICE_ROLE_KEY") ??
    Deno.env.get("SUPABASE_SECRET_KEY") ??
    "";
  if (!url || !anonKey || !serviceKey || !authHeader.toLowerCase().startsWith("bearer ")) {
    return json({ error: "Sign in again." }, 401);
  }

  const userClient = createClient(url, anonKey, {
    global: { headers: { Authorization: authHeader } },
    auth: { persistSession: false, autoRefreshToken: false },
  });
  const { data: userData, error: userError } = await userClient.auth.getUser();
  const email = userData.user?.email;
  if (userError || !email) {
    return json({ error: "Sign in again." }, 401);
  }

  const admin = createClient(url, serviceKey, {
    auth: { persistSession: false, autoRefreshToken: false },
  });
  const { data, error } = await admin.auth.admin.generateLink({
    type: "magiclink",
    email,
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
});
