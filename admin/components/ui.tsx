import Link from "next/link";

export function cx(...parts: Array<string | false | null | undefined>) {
  return parts.filter(Boolean).join(" ");
}

export const fieldClass =
  "w-full rounded-md border border-zinc-200 bg-white px-3 py-2 text-sm text-zinc-900 outline-none placeholder:text-zinc-400 focus:border-zinc-400 focus:ring-2 focus:ring-zinc-900/10";

export function Field({
  label,
  children,
  hint,
}: {
  label: string;
  children: React.ReactNode;
  hint?: string;
}) {
  return (
    <label className="block text-sm">
      <span className="mb-1.5 block font-medium text-zinc-700">{label}</span>
      {children}
      {hint ? <span className="mt-1 block text-xs text-zinc-500">{hint}</span> : null}
    </label>
  );
}

export function Button({
  children,
  variant = "primary",
  className,
  ...props
}: React.ButtonHTMLAttributes<HTMLButtonElement> & {
  variant?: "primary" | "secondary" | "danger" | "ghost";
}) {
  const styles = {
    primary: "bg-zinc-900 text-white hover:bg-zinc-800",
    secondary: "border border-zinc-200 bg-white text-zinc-900 hover:bg-zinc-50",
    danger: "border border-red-200 bg-white text-red-700 hover:bg-red-50",
    ghost: "text-zinc-600 hover:bg-zinc-100 hover:text-zinc-900",
  }[variant];
  return (
    <button
      className={cx(
        "inline-flex items-center justify-center rounded-md px-3 py-2 text-sm font-medium disabled:opacity-50",
        styles,
        className,
      )}
      {...props}
    >
      {children}
    </button>
  );
}

export function ButtonLink({
  href,
  children,
  variant = "primary",
}: {
  href: string;
  children: React.ReactNode;
  variant?: "primary" | "secondary";
}) {
  return (
    <Link
      href={href}
      className={cx(
        "inline-flex items-center justify-center rounded-md px-3 py-2 text-sm font-medium",
        variant === "primary"
          ? "bg-zinc-900 text-white hover:bg-zinc-800"
          : "border border-zinc-200 bg-white text-zinc-900 hover:bg-zinc-50",
      )}
    >
      {children}
    </Link>
  );
}

export function Badge({
  children,
  tone = "zinc",
}: {
  children: React.ReactNode;
  tone?: "zinc" | "green" | "amber" | "red" | "blue";
}) {
  const styles = {
    zinc: "bg-zinc-100 text-zinc-700",
    green: "bg-emerald-50 text-emerald-700",
    amber: "bg-amber-50 text-amber-800",
    red: "bg-red-50 text-red-700",
    blue: "bg-sky-50 text-sky-800",
  }[tone];
  return (
    <span className={cx("inline-flex rounded-md px-1.5 py-0.5 text-xs font-medium", styles)}>
      {children}
    </span>
  );
}

export function PageHeader({
  eyebrow,
  title,
  description,
  actions,
}: {
  eyebrow?: string;
  title: string;
  description?: string;
  actions?: React.ReactNode;
}) {
  return (
    <div className="mb-6 flex flex-wrap items-start justify-between gap-4">
      <div>
        {eyebrow ? <p className="text-xs font-medium uppercase tracking-wide text-zinc-500">{eyebrow}</p> : null}
        <h1 className="mt-1 text-xl font-semibold tracking-tight text-zinc-900">{title}</h1>
        {description ? <p className="mt-1 max-w-2xl text-sm text-zinc-500">{description}</p> : null}
      </div>
      {actions ? <div className="flex flex-wrap gap-2">{actions}</div> : null}
    </div>
  );
}

export function Card({ children, className }: { children: React.ReactNode; className?: string }) {
  return (
    <div className={cx("rounded-xl border border-zinc-200 bg-white", className)}>{children}</div>
  );
}

export function Alert({ children, tone = "error" }: { children: React.ReactNode; tone?: "error" | "ok" }) {
  return (
    <p className={cx("text-sm", tone === "error" ? "text-red-600" : "text-emerald-700")}>{children}</p>
  );
}
