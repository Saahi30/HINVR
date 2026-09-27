import "jsr:@supabase/functions-js/edge-runtime.d.ts";
import { googleWalletSaveUrl } from "../_shared/google-wallet.ts";
import { json } from "../_shared/http.ts";
import { issuePass } from "../_shared/issue-pass.ts";

Deno.serve(async (req: Request) => {
  if (req.method !== "POST") return json({ error: "Couldn't open Google Wallet." }, 405);
  try {
    const issued = await issuePass(req, "wallet", false);
    if (!issued.ok) return issued.response;
    const saved = await googleWalletSaveUrl(issued.pass);
    if ("error" in saved) return json({ error: saved.error }, saved.status);
    return json({ url: saved.url, expires_at: issued.pass.expiresAt });
  } catch {
    return json({ error: "Couldn't open Google Wallet." }, 500);
  }
});
