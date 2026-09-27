import { bytesToB64url } from "./pass-token.ts";
import type { IssuedPass } from "./issue-pass.ts";

type ServiceAccount = {
  client_email: string;
  private_key: string;
};

const WALLET_SCOPE = "https://www.googleapis.com/auth/wallet_object.issuer";

function readServiceAccount(): ServiceAccount | null {
  const issuer = Deno.env.get("GOOGLE_WALLET_ISSUER_ID")?.trim() ?? "";
  const raw = Deno.env.get("GOOGLE_WALLET_SERVICE_ACCOUNT")?.trim() ?? "";
  if (!issuer || !raw) return null;
  const text = raw.startsWith("{") ? raw : new TextDecoder().decode(bytesFromB64(raw));
  const parsed = JSON.parse(text) as ServiceAccount;
  if (!parsed.client_email || !parsed.private_key) return null;
  return parsed;
}

function bytesFromB64(value: string): Uint8Array {
  const padded = value.replaceAll("-", "+").replaceAll("_", "/") + "===".slice((value.length + 3) % 4);
  const binary = atob(padded);
  const bytes = new Uint8Array(binary.length);
  for (let index = 0; index < binary.length; index += 1) bytes[index] = binary.charCodeAt(index);
  return bytes;
}

function pemToPkcs8(pem: string): ArrayBuffer {
  const body = pem.replace(/-----BEGIN PRIVATE KEY-----/g, "").replace(/-----END PRIVATE KEY-----/g, "").replace(/\s/g, "");
  return bytesFromB64(body).buffer;
}

async function importGoogleKey(pem: string): Promise<CryptoKey> {
  return crypto.subtle.importKey(
    "pkcs8",
    pemToPkcs8(pem),
    { name: "RSASSA-PKCS1-v1_5", hash: "SHA-256" },
    false,
    ["sign"],
  );
}

async function signJwt(key: CryptoKey, claims: Record<string, unknown>): Promise<string> {
  const header = bytesToB64url(new TextEncoder().encode(JSON.stringify({ alg: "RS256", typ: "JWT" })));
  const body = bytesToB64url(new TextEncoder().encode(JSON.stringify(claims)));
  const signature = new Uint8Array(
    await crypto.subtle.sign("RSASSA-PKCS1-v1_5", key, new TextEncoder().encode(`${header}.${body}`)),
  );
  return `${header}.${body}.${bytesToB64url(signature)}`;
}

async function accessToken(account: ServiceAccount, key: CryptoKey): Promise<string> {
  const now = Math.floor(Date.now() / 1000);
  const assertion = await signJwt(key, {
    iss: account.client_email,
    scope: WALLET_SCOPE,
    aud: "https://oauth2.googleapis.com/token",
    iat: now,
    exp: now + 3600,
  });
  const response = await fetch("https://oauth2.googleapis.com/token", {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: new URLSearchParams({
      grant_type: "urn:ietf:params:oauth:grant-type:jwt-bearer",
      assertion,
    }),
  });
  const body = await response.json().catch(() => ({}));
  if (!response.ok || !body.access_token) {
    throw new Error("Google Wallet did not accept the issuer account.");
  }
  return String(body.access_token);
}

export async function googleWalletSaveUrl(pass: IssuedPass): Promise<{ url: string } | { error: string; status: number }> {
  const issuer = Deno.env.get("GOOGLE_WALLET_ISSUER_ID")?.trim() ?? "";
  const account = readServiceAccount();
  if (!issuer || !account) {
    return { error: "Google Wallet is not set up for this club yet.", status: 503 };
  }
  const key = await importGoogleKey(account.private_key);
  const token = await accessToken(account, key);
  const classId = `${issuer}.hinvr_membership`;
  const classResponse = await fetch("https://walletobjects.googleapis.com/walletobjects/v1/genericClass", {
    method: "POST",
    headers: {
      Authorization: `Bearer ${token}`,
      "Content-Type": "application/json",
    },
    body: JSON.stringify({
      id: classId,
      issuerName: "HINVR",
      reviewStatus: "UNDER_REVIEW",
    }),
  });
  if (!classResponse.ok && classResponse.status !== 409) {
    return { error: "Google Wallet did not open the HINVR pass.", status: 502 };
  }

  const objectId = `${issuer}.hinvr_${pass.memberId.replace(/[^a-zA-Z0-9._-]/g, "_")}`;
  const now = Math.floor(Date.now() / 1000);
  const save = await signJwt(key, {
    iss: account.client_email,
    aud: "google",
    typ: "savetowallet",
    iat: now,
    payload: {
      genericObjects: [
        {
          id: objectId,
          classId,
          state: "ACTIVE",
          hexBackgroundColor: "#1A120C",
          cardTitle: { defaultValue: { language: "en-US", value: "HINVR" } },
          header: { defaultValue: { language: "en-US", value: pass.displayName } },
          subheader: { defaultValue: { language: "en-US", value: `${pass.tier === "Nri" ? "NRI" : pass.tier} pass` } },
          barcode: { type: "QR_CODE", value: pass.token, alternateText: pass.memberId },
          textModulesData: [
            { id: "member", header: "Member", body: pass.memberId },
            { id: "valid", header: "Valid through", body: pass.validUntil || "This year" },
            { id: "code", header: "Code expires", body: "Reopen HINVR to refresh this code" },
          ],
          validTimeInterval: {
            start: { date: new Date(now * 1000).toISOString() },
            end: { date: pass.expiresAt },
          },
        },
      ],
    },
  });
  return { url: `https://pay.google.com/gp/v/save/${save}` };
}
