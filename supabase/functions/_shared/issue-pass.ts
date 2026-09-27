import { createClient, type SupabaseClient } from "jsr:@supabase/supabase-js@2";
import { json } from "./http.ts";
import {
  DESK_TTL_SECONDS,
  WALLET_TTL_SECONDS,
  membershipEndUnix,
  signPassToken,
  type PassClaims,
  type PassPurpose,
} from "./pass-token.ts";

const PASS_TIERS = new Set(["Gold", "Platinum", "Nri"]);

export type IssuedPass = {
  token: string;
  expiresAt: string;
  memberId: string;
  tier: string;
  displayName: string;
  validUntil: string;
  generation: number;
};

export type IssueResult = { ok: true; pass: IssuedPass } | { ok: false; response: Response };

type Profile = {
  id: string;
  display_name: string;
  tier: string;
  member_id: string;
  valid_until: string;
  pass_generation: number;
};

async function signingMaterial(admin: SupabaseClient): Promise<{ kid: string; privateJwk: JsonWebKey }> {
  const pair = await crypto.subtle.generateKey({ name: "ECDSA", namedCurve: "P-256" }, true, ["sign", "verify"]);
  const privateJwk = await crypto.subtle.exportKey("jwk", pair.privateKey);
  const publicJwk = await crypto.subtle.exportKey("jwk", pair.publicKey);
  delete publicJwk.d;
  const { data, error } = await admin.rpc("ensure_pass_signing_key", {
    p_private: privateJwk,
    p_public: publicJwk,
  });
  if (error || !data?.private_jwk) {
    throw new Error(error?.message || "Couldn't open the pass key.");
  }
  return { kid: String(data.kid || "primary"), privateJwk: data.private_jwk as JsonWebKey };
}

export async function issuePass(req: Request, purpose: PassPurpose, rotate: boolean): Promise<IssueResult> {
  const authHeader = req.headers.get("Authorization") ?? "";
  const url = Deno.env.get("SUPABASE_URL") ?? "";
  const anonKey = Deno.env.get("SUPABASE_ANON_KEY") ?? "";
  const serviceKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY") ?? Deno.env.get("SUPABASE_SECRET_KEY") ?? "";
  if (!url || !anonKey || !serviceKey || !authHeader.toLowerCase().startsWith("bearer ")) {
    return { ok: false, response: json({ error: "Sign in again." }, 401) };
  }

  const userClient = createClient(url, anonKey, {
    global: { headers: { Authorization: authHeader } },
    auth: { persistSession: false, autoRefreshToken: false },
  });
  const { data: userData, error: userError } = await userClient.auth.getUser();
  const userId = userData.user?.id;
  if (userError || !userId) {
    return { ok: false, response: json({ error: "Sign in again." }, 401) };
  }

  const admin = createClient(url, serviceKey, {
    auth: { persistSession: false, autoRefreshToken: false },
  });
  const { data: profile, error: profileError } = await admin
    .from("profiles")
    .select("id,display_name,tier,member_id,valid_until,pass_generation")
    .eq("id", userId)
    .maybeSingle();
  if (profileError || !profile) {
    return { ok: false, response: json({ error: "Couldn't open this membership." }, 500) };
  }
  const row = profile as Profile;
  if (!PASS_TIERS.has(row.tier)) {
    return { ok: false, response: json({ error: "Gold, Platinum, or NRI opens the pass." }, 403) };
  }
  const memberId = row.member_id.trim();
  if (!memberId) {
    return { ok: false, response: json({ error: "The desk has not issued a member ID yet." }, 403) };
  }

  const { data: purchase } = await admin
    .from("membership_purchases")
    .select("valid_until")
    .eq("user_id", userId)
    .order("purchased_on", { ascending: false })
    .limit(1)
    .maybeSingle();
  const membershipEnd = membershipEndUnix(purchase?.valid_until || "") ?? membershipEndUnix(row.valid_until || "");
  if (membershipEnd == null) {
    return { ok: false, response: json({ error: "The desk has not set a validity date." }, 403) };
  }

  const now = Math.floor(Date.now() / 1000);
  const ttl = purpose === "wallet" ? WALLET_TTL_SECONDS : DESK_TTL_SECONDS;
  const exp = Math.min(now + ttl, membershipEnd);
  if (exp <= now + 60) {
    return { ok: false, response: json({ error: "This membership has ended." }, 403) };
  }

  let generation = Number(row.pass_generation) || 0;
  if (rotate) {
    const rotated = await admin.rpc("rotate_member_pass", { p_user_id: userId });
    if (rotated.error || rotated.data == null) {
      return { ok: false, response: json({ error: "Couldn't rotate this pass." }, 500) };
    }
    generation = Number(rotated.data);
  } else {
    const revoked = await admin
      .from("pass_credentials")
      .update({ revoked_at: new Date().toISOString() })
      .eq("user_id", userId)
      .eq("purpose", purpose)
      .is("revoked_at", null);
    if (revoked.error) {
      return { ok: false, response: json({ error: "Couldn't refresh this pass." }, 500) };
    }
  }

  const id = crypto.randomUUID();
  const expiresAt = new Date(exp * 1000).toISOString();
  const inserted = await admin.from("pass_credentials").insert({
    id,
    user_id: userId,
    generation,
    member_id: memberId,
    tier: row.tier,
    purpose,
    expires_at: expiresAt,
  });
  if (inserted.error) {
    return { ok: false, response: json({ error: "Couldn't refresh this pass." }, 500) };
  }

  let material: { kid: string; privateJwk: JsonWebKey };
  try {
    material = await signingMaterial(admin);
  } catch (error) {
    const message = error instanceof Error ? error.message : "Couldn't open the pass key.";
    return { ok: false, response: json({ error: message }, 500) };
  }

  const claims: PassClaims = {
    j: id,
    u: userId,
    m: memberId,
    t: row.tier,
    v: membershipEnd,
    e: exp,
    g: generation,
    p: purpose,
  };
  const token = await signPassToken(material.privateJwk, material.kid, claims);
  return {
    ok: true,
    pass: {
      token,
      expiresAt,
      memberId,
      tier: row.tier,
      displayName: row.display_name?.trim() || "Member",
      validUntil: row.valid_until?.trim() || "",
      generation,
    },
  };
}
