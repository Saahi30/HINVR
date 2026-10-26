import { createClient, type SupabaseClient } from "jsr:@supabase/supabase-js@2";

export type StaffRole = "owner" | "admin" | "finance" | "desk" | "editor";

export type Caller = {
  userId: string;
  email: string;
  /** Seconds since the access token was minted. */
  tokenAgeSeconds: number | null;
};

export type Env = { url: string; anonKey: string; serviceKey: string };

export function readEnv(): Env | null {
  const url = Deno.env.get("SUPABASE_URL") ?? "";
  const anonKey = Deno.env.get("SUPABASE_ANON_KEY") ?? "";
  const serviceKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY") ?? Deno.env.get("SUPABASE_SECRET_KEY") ?? "";
  if (!url || !anonKey || !serviceKey) return null;
  return { url, anonKey, serviceKey };
}

export function serviceClient(env: Env): SupabaseClient {
  return createClient(env.url, env.serviceKey, {
    auth: { persistSession: false, autoRefreshToken: false },
  });
}

function tokenIssuedAt(authHeader: string): number | null {
  const token = authHeader.replace(/^bearer\s+/i, "");
  const part = token.split(".")[1];
  if (!part) return null;
  try {
    const padded = part.replaceAll("-", "+").replaceAll("_", "/") + "===".slice((part.length + 3) % 4);
    const claims = JSON.parse(atob(padded)) as { iat?: number };
    return Number.isFinite(claims.iat) ? Number(claims.iat) : null;
  } catch {
    return null;
  }
}

/** The signed-in user behind the request, checked with Supabase Auth. Null when signed out. */
export async function getCaller(req: Request, env: Env): Promise<Caller | null> {
  const authHeader = req.headers.get("Authorization") ?? "";
  if (!authHeader.toLowerCase().startsWith("bearer ")) return null;
  const userClient = createClient(env.url, env.anonKey, {
    global: { headers: { Authorization: authHeader } },
    auth: { persistSession: false, autoRefreshToken: false },
  });
  const { data, error } = await userClient.auth.getUser();
  if (error || !data.user?.id) return null;
  const iat = tokenIssuedAt(authHeader);
  return {
    userId: data.user.id,
    email: data.user.email ?? "",
    tokenAgeSeconds: iat == null ? null : Math.floor(Date.now() / 1000) - iat,
  };
}

export async function staffRole(admin: SupabaseClient, userId: string): Promise<StaffRole | null> {
  const { data, error } = await admin.from("staff").select("role").eq("user_id", userId).maybeSingle();
  if (error || !data) return null;
  return data.role as StaffRole;
}

export function hasRole(role: StaffRole | null, allowed: readonly StaffRole[]): boolean {
  return role != null && allowed.includes(role);
}
