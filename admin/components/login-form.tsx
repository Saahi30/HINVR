"use client";

import { useRouter } from "next/navigation";
import { useState } from "react";
import { Alert, Button, Field, fieldClass } from "@/components/ui";
import { createClient, isSupabaseConfigured } from "@/lib/supabase/client";

export function LoginForm() {
  const router = useRouter();
  const [mode, setMode] = useState<"signin" | "signup">("signin");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [message, setMessage] = useState("");
  const [busy, setBusy] = useState(false);
  const configured = isSupabaseConfigured();

  async function onSubmit(event: React.FormEvent) {
    event.preventDefault();
    setMessage("");
    if (!configured) {
      setMessage("Add NEXT_PUBLIC_SUPABASE_URL and NEXT_PUBLIC_SUPABASE_PUBLISHABLE_KEY to admin/.env.local.");
      return;
    }
    setBusy(true);
    try {
      const supabase = createClient();
      if (mode === "signin") {
        const { error } = await supabase.auth.signInWithPassword({ email, password });
        if (error) throw error;
        router.replace("/");
        router.refresh();
        return;
      }
      const origin = window.location.origin;
      const { data, error } = await supabase.auth.signUp({
        email,
        password,
        options: { emailRedirectTo: `${origin}/auth/callback` },
      });
      if (error) throw error;
      if (!data.session) {
        setMessage("Confirm the email, then sign in. Or disable Confirm email in Supabase Auth.");
        return;
      }
      router.replace("/");
      router.refresh();
    } catch (error) {
      setMessage(error instanceof Error ? error.message : "Could not sign in.");
    } finally {
      setBusy(false);
    }
  }

  return (
    <form onSubmit={onSubmit} className="rounded-xl border border-zinc-200 bg-white p-5 shadow-sm">
      <div className="mb-5 grid grid-cols-2 gap-1 rounded-lg bg-zinc-100 p-1 text-sm">
        <button
          type="button"
          onClick={() => setMode("signin")}
          className={mode === "signin" ? "rounded-md bg-white py-1.5 font-medium shadow-sm" : "rounded-md py-1.5 text-zinc-500"}
        >
          Sign in
        </button>
        <button
          type="button"
          onClick={() => setMode("signup")}
          className={mode === "signup" ? "rounded-md bg-white py-1.5 font-medium shadow-sm" : "rounded-md py-1.5 text-zinc-500"}
        >
          Create account
        </button>
      </div>
      <div className="space-y-4">
        <Field label="Work email">
          <input
            type="email"
            required
            autoComplete="email"
            value={email}
            onChange={(event) => setEmail(event.target.value)}
            className={fieldClass}
          />
        </Field>
        <Field label="Password">
          <input
            type="password"
            required
            minLength={6}
            autoComplete={mode === "signin" ? "current-password" : "new-password"}
            value={password}
            onChange={(event) => setPassword(event.target.value)}
            className={fieldClass}
          />
        </Field>
      </div>
      {message ? (
        <div className="mt-4">
          <Alert>{message}</Alert>
        </div>
      ) : null}
      <Button type="submit" disabled={busy} className="mt-5 w-full">
        {busy ? "Working…" : mode === "signin" ? "Continue" : "Create first admin"}
      </Button>
      <p className="mt-4 text-xs leading-5 text-zinc-500">
        Add <code className="rounded bg-zinc-100 px-1">http://localhost:3000/auth/callback</code> to Auth redirect URLs.
      </p>
    </form>
  );
}
