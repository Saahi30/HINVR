type ServiceAccount = {
  client_email: string;
  private_key: string;
  project_id: string;
};

const FCM_SCOPE = "https://www.googleapis.com/auth/firebase.messaging";

function bytesToB64url(bytes: Uint8Array): string {
  let binary = "";
  for (const byte of bytes) binary += String.fromCharCode(byte);
  return btoa(binary).replaceAll("+", "-").replaceAll("/", "_").replace(/=+$/g, "");
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

function readServiceAccount(): ServiceAccount | null {
  const raw = Deno.env.get("FIREBASE_SERVICE_ACCOUNT")?.trim() ?? "";
  if (!raw) return null;
  try {
    const text = raw.startsWith("{") ? raw : new TextDecoder().decode(bytesFromB64(raw));
    const parsed = JSON.parse(text) as ServiceAccount;
    if (!parsed.client_email || !parsed.private_key || !parsed.project_id) return null;
    return parsed;
  } catch {
    return null;
  }
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

async function accessToken(account: ServiceAccount): Promise<string> {
  const key = await importGoogleKey(account.private_key);
  const now = Math.floor(Date.now() / 1000);
  const assertion = await signJwt(key, {
    iss: account.client_email,
    scope: FCM_SCOPE,
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
    throw new Error("Firebase did not accept the sender key.");
  }
  return String(body.access_token);
}

export type PushResult = { ok: true } | { ok: false; drop: boolean };

export async function sendPush(input: {
  tokens: string[];
  title: string;
  body: string;
  notificationId: string;
}): Promise<{ sent: number; failed: number; drop: string[] }> {
  const account = readServiceAccount();
  if (!account) throw new Error("Firebase sender key is missing.");
  const bearer = await accessToken(account);
  const endpoint = `https://fcm.googleapis.com/v1/projects/${account.project_id}/messages:send`;
  let sent = 0;
  let failed = 0;
  const drop: string[] = [];
  let cursor = 0;
  const workers = Array.from({ length: Math.min(8, input.tokens.length) }, async () => {
    while (cursor < input.tokens.length) {
      const token = input.tokens[cursor];
      cursor += 1;
      const result = await postMessage(endpoint, bearer, token, input);
      if (result.ok) sent += 1;
      else {
        failed += 1;
        if (result.drop) drop.push(token);
      }
    }
  });
  await Promise.all(workers);
  return { sent, failed, drop };
}

async function postMessage(
  endpoint: string,
  bearer: string,
  token: string,
  input: { title: string; body: string; notificationId: string },
): Promise<PushResult> {
  const response = await fetch(endpoint, {
    method: "POST",
    headers: {
      Authorization: `Bearer ${bearer}`,
      "Content-Type": "application/json",
    },
    body: JSON.stringify({
      message: {
        token,
        notification: { title: input.title, body: input.body },
        data: { open: "notifications", notification_id: input.notificationId },
        android: {
          priority: "HIGH",
          notification: { channel_id: "aarti" },
        },
      },
    }),
  });
  if (response.ok) return { ok: true };
  const payload = await response.json().catch(() => ({}));
  const status = String(payload?.error?.status ?? "");
  const details = JSON.stringify(payload?.error?.details ?? []);
  const drop = response.status === 404 || status === "NOT_FOUND" || status === "UNREGISTERED" || details.includes("UNREGISTERED");
  return { ok: false, drop };
}
