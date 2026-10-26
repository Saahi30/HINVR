import { NextResponse } from "next/server";
import { safeNextPath } from "@/lib/redirect";
import { createClient } from "@/lib/supabase/server";

function toLogin(origin: string, message: string) {
  const target = new URL("/login", origin);
  target.searchParams.set("error", message);
  return NextResponse.redirect(target);
}

export async function GET(request: Request) {
  const url = new URL(request.url);
  const code = url.searchParams.get("code");
  const next = safeNextPath(url.searchParams.get("next"));
  const authError = url.searchParams.get("error_description") || url.searchParams.get("error");
  if (authError) return toLogin(url.origin, authError.slice(0, 200));
  if (!code) return toLogin(url.origin, "That sign-in link is incomplete. Sign in again.");

  const supabase = await createClient();
  const { error } = await supabase.auth.exchangeCodeForSession(code);
  if (error) {
    return toLogin(url.origin, "That sign-in link has expired or was already used. Sign in again.");
  }
  return NextResponse.redirect(new URL(next, url.origin));
}
