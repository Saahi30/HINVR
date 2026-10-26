import "jsr:@supabase/functions-js/edge-runtime.d.ts";
import { json, readJson, withCors } from "../_shared/http.ts";
import { issuePass } from "../_shared/issue-pass.ts";

// Desk tokens only. Wallet tokens (24 h) come from pass-wallet, which also updates the Wallet object.
Deno.serve(withCors(async (req: Request) => {
  if (req.method !== "POST") return json({ error: "Couldn't open the pass." }, 405);
  const body = await readJson(req);
  const rotate = body.rotate === true;
  try {
    const issued = await issuePass(req, "desk", rotate);
    if (!issued.ok) return issued.response;
    return json({
      token: issued.pass.token,
      expires_at: issued.pass.expiresAt,
      member_id: issued.pass.memberId,
      tier: issued.pass.tier,
      generation: issued.pass.generation,
    });
  } catch (error) {
    console.error("pass-credential", error instanceof Error ? error.message : error);
    return json({ error: "Couldn't refresh this pass." }, 500);
  }
}));
