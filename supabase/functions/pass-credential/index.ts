import "jsr:@supabase/functions-js/edge-runtime.d.ts";
import { json } from "../_shared/http.ts";
import { issuePass } from "../_shared/issue-pass.ts";
import type { PassPurpose } from "../_shared/pass-token.ts";

Deno.serve(async (req: Request) => {
  if (req.method !== "POST") return json({ error: "Couldn't open the pass." }, 405);
  let rotate = false;
  let purpose: PassPurpose = "desk";
  try {
    const body = await req.json();
    rotate = body?.rotate === true;
    if (body?.purpose === "wallet") purpose = "wallet";
  } catch {
    rotate = false;
  }
  try {
    const issued = await issuePass(req, purpose, rotate);
    if (!issued.ok) return issued.response;
    return json({
      token: issued.pass.token,
      expires_at: issued.pass.expiresAt,
      member_id: issued.pass.memberId,
      tier: issued.pass.tier,
      generation: issued.pass.generation,
    });
  } catch {
    return json({ error: "Couldn't refresh this pass." }, 500);
  }
});
