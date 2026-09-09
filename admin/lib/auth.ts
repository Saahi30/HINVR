import { redirect } from "next/navigation";
import { createClient } from "@/lib/supabase/server";
import type { Staff } from "@/lib/types";

export function isMissingRelation(error: { code?: string; message?: string } | null) {
  if (!error) return false;
  const message = error.message ?? "";
  return (
    error.code === "PGRST205" ||
    error.code === "42P01" ||
    /schema cache|does not exist|could not find the table/i.test(message)
  );
}

export async function requireDesk() {
  const supabase = await createClient();
  const { data } = await supabase.auth.getClaims();
  if (!data?.claims) {
    redirect("/login");
  }

  const email = String(data.claims.email ?? "");
  const userId = String(data.claims.sub ?? "");

  const { error: tableError } = await supabase.from("mandirs").select("id").limit(1);
  if (isMissingRelation(tableError)) {
    return { supabase, email, userId, staff: null as Staff | null, needsSetup: true };
  }

  await supabase.rpc("bootstrap_staff");
  const { data: staff } = await supabase
    .from("staff")
    .select("user_id, email, role")
    .eq("user_id", userId)
    .maybeSingle();

  return {
    supabase,
    email,
    userId,
    staff: staff as Staff | null,
    needsSetup: false,
  };
}
