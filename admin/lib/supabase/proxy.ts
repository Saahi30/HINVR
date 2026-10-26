import { createServerClient } from "@supabase/ssr";
import { NextResponse, type NextRequest } from "next/server";
import { safeNextPath } from "@/lib/redirect";

export async function updateSession(request: NextRequest) {
  let supabaseResponse = NextResponse.next({ request });
  const url = process.env.NEXT_PUBLIC_SUPABASE_URL;
  const key = process.env.NEXT_PUBLIC_SUPABASE_PUBLISHABLE_KEY;
  if (!url || !key) {
    return supabaseResponse;
  }

  const supabase = createServerClient(url, key, {
    cookies: {
      getAll() {
        return request.cookies.getAll();
      },
      setAll(cookiesToSet) {
        cookiesToSet.forEach(({ name, value }) => request.cookies.set(name, value));
        supabaseResponse = NextResponse.next({ request });
        cookiesToSet.forEach(({ name, value, options }) =>
          supabaseResponse.cookies.set(name, value, options),
        );
      },
    },
  });

  const { data } = await supabase.auth.getClaims();
  const signedIn = Boolean(data?.claims);
  const path = request.nextUrl.pathname;
  const publicPath =
    path.startsWith("/login") || path.startsWith("/auth");

  if (!signedIn && !publicPath) {
    const redirectUrl = request.nextUrl.clone();
    redirectUrl.pathname = "/login";
    redirectUrl.search = "";
    const next = safeNextPath(`${path}${request.nextUrl.search}`, "");
    if (next && next !== "/") redirectUrl.searchParams.set("next", next);
    return NextResponse.redirect(redirectUrl);
  }

  if (signedIn && path.startsWith("/login")) {
    const target = new URL(safeNextPath(request.nextUrl.searchParams.get("next")), request.nextUrl.origin);
    return NextResponse.redirect(target);
  }

  return supabaseResponse;
}
