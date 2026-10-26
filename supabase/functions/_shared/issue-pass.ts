import { json } from "./http.ts";
import { getCaller, readEnv, serviceClient } from "./auth.ts";
import { activeSigningKey, SigningConfigError } from "./signing-keys.ts";
import {
  DESK_TTL_SECONDS,
  WALLET_TTL_SECONDS,
  signPassToken,
  type PassClaims,
  type PassPurpose,
} from "./pass-token.ts";

export type IssuedPass = {
  token: string;
  expiresAt: string;
  memberId: string;
  tier: string;
  displayName: string;
  /** "28 Sep 2027" */
  validUntil: string;
  generation: number;
};

export type IssueResult = { ok: true; pass: IssuedPass } | { ok: false; response: Response };

type Issued = {
  id: string;
  user_id: string;
  generation: number;
  member_id: string;
  tier: string;
  purpose: PassPurpose;
  expires_at: string;
  expires_unix: number;
  membership_end_unix: number;
  valid_until: string;
  display_name: string;
};

/** Errors raised by issue_pass_credential carry a hint the member can act on. */
const MEMBER_HINTS = new Set(["no_member", "pass_tier", "member_id", "validity", "ended"]);

function displayDate(iso: string) {
  const date = new Date(`${iso.slice(0, 10)}T00:00:00Z`);
  if (Number.isNaN(date.getTime())) return "";
  return date.toLocaleDateString("en-GB", { day: "numeric", month: "short", year: "numeric", timeZone: "UTC" });
}

/**
 * Issues a pass for the signed-in member. The database validates the membership, retires
 * older credentials (rotate: all of them; otherwise beyond the newest few) and records the
 * new one in one transaction. The token is then signed with the key from the function secret.
 */
export async function issuePass(req: Request, purpose: PassPurpose, rotate: boolean): Promise<IssueResult> {
  const env = readEnv();
  if (!env) return { ok: false, response: json({ error: "Function isn't configured." }, 500) };
  const caller = await getCaller(req, env);
  if (!caller) return { ok: false, response: json({ error: "Sign in again." }, 401) };

  const admin = serviceClient(env);
  let signing: { kid: string; privateJwk: JsonWebKey };
  try {
    signing = await activeSigningKey(admin);
  } catch (error) {
    const message = error instanceof SigningConfigError ? error.message : "Couldn't open the pass key.";
    console.error("pass signing", message);
    return { ok: false, response: json({ error: "The pass is not available right now." }, 503) };
  }

  const { data, error } = await admin.rpc("issue_pass_credential", {
    p_user_id: caller.userId,
    p_purpose: purpose,
    p_rotate: rotate,
    p_ttl_seconds: purpose === "wallet" ? WALLET_TTL_SECONDS : DESK_TTL_SECONDS,
  });
  if (error || !data) {
    if (error?.hint && MEMBER_HINTS.has(error.hint)) {
      return { ok: false, response: json({ error: error.message }, error.hint === "no_member" ? 404 : 403) };
    }
    console.error("issue_pass_credential", error?.message);
    return { ok: false, response: json({ error: "Couldn't refresh this pass." }, 500) };
  }
  const issued = data as Issued;

  const claims: PassClaims = {
    j: issued.id,
    u: issued.user_id,
    m: issued.member_id,
    t: issued.tier,
    v: Number(issued.membership_end_unix),
    e: Number(issued.expires_unix),
    g: Number(issued.generation),
    p: purpose,
  };
  const token = await signPassToken(signing.privateJwk, signing.kid, claims);
  return {
    ok: true,
    pass: {
      token,
      expiresAt: new Date(claims.e * 1000).toISOString(),
      memberId: issued.member_id,
      tier: issued.tier,
      displayName: issued.display_name || "Member",
      validUntil: displayDate(String(issued.valid_until ?? "")),
      generation: claims.g,
    },
  };
}
