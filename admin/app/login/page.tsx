import { LoginForm } from "@/components/login-form";

export default function LoginPage() {
  return (
    <div className="min-h-full lg:grid lg:grid-cols-2">
      <div className="hidden bg-sidebar px-12 py-10 text-white lg:flex lg:flex-col lg:justify-between">
        <div className="flex items-center gap-2.5">
          <div className="flex h-8 w-8 items-center justify-center rounded-md bg-white text-xs font-semibold text-zinc-950">
            H
          </div>
          <div>
            <p className="text-sm font-semibold">HINVR</p>
            <p className="text-xs text-zinc-500">Admin</p>
          </div>
        </div>
        <div>
          <p className="text-xs font-medium tracking-[0.16em] text-zinc-500 uppercase">Control plane</p>
          <h1 className="mt-3 max-w-md text-3xl font-semibold tracking-tight">
            Publish the catalog the Android app loads.
          </h1>
          <ul className="mt-8 space-y-3 text-sm text-zinc-400">
            <li>Add or remove mandirs, photos, and stream URLs</li>
            <li>Edit the home grid — tiles appear in the same order</li>
            <li>Set membership tiers and pass dates</li>
          </ul>
        </div>
        <p className="text-xs text-zinc-600">Staff only. First account becomes owner.</p>
      </div>
      <div className="flex min-h-full items-center justify-center bg-zinc-50 px-6 py-16">
        <div className="w-full max-w-sm">
          <p className="text-[11px] font-medium tracking-[0.16em] text-zinc-500 uppercase lg:hidden">HINVR Admin</p>
          <h2 className="mt-2 text-xl font-semibold tracking-tight text-zinc-900">Sign in</h2>
          <p className="mt-1 text-sm text-zinc-500">Use the desk account for this project.</p>
          <div className="mt-6">
            <LoginForm />
          </div>
        </div>
      </div>
    </div>
  );
}
