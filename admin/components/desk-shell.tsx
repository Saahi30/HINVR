"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { useState } from "react";
import { IconClose, IconGrid, IconHome, IconMenu, IconShield, IconTemple, IconUsers } from "@/components/icons";
import { cx } from "@/components/ui";

const catalog = [
  { href: "/", label: "Overview", icon: IconGrid, exact: true },
  { href: "/mandirs", label: "Mandirs", icon: IconTemple },
  { href: "/home", label: "Home screen", icon: IconHome },
];

const audience = [{ href: "/members", label: "Members", icon: IconUsers }];
const workspace = [{ href: "/staff", label: "Team", icon: IconShield }];

function crumbs(pathname: string) {
  if (pathname === "/") return ["Overview"];
  if (pathname === "/mandirs/new") return ["Catalog", "Mandirs", "New"];
  if (pathname.startsWith("/mandirs/")) return ["Catalog", "Mandirs", "Edit"];
  if (pathname.startsWith("/mandirs")) return ["Catalog", "Mandirs"];
  if (pathname.startsWith("/home")) return ["Catalog", "Home screen"];
  if (pathname.startsWith("/members")) return ["Audience", "Members"];
  if (pathname.startsWith("/staff")) return ["Workspace", "Team"];
  if (pathname.startsWith("/setup")) return ["Workspace", "Setup"];
  return ["Admin"];
}

function NavList({
  pathname,
  items,
  onNavigate,
}: {
  pathname: string;
  items: Array<{ href: string; label: string; icon: typeof IconGrid; exact?: boolean }>;
  onNavigate?: () => void;
}) {
  return (
    <div className="space-y-0.5">
      {items.map((item) => {
        const active = item.exact ? pathname === item.href : pathname === item.href || pathname.startsWith(`${item.href}/`);
        const Icon = item.icon;
        return (
          <Link
            key={item.href}
            href={item.href}
            onClick={onNavigate}
            className={cx(
              "flex items-center gap-2.5 rounded-md px-2.5 py-1.5 text-[13px]",
              active ? "bg-white/10 text-white" : "text-zinc-400 hover:bg-white/5 hover:text-zinc-100",
            )}
          >
            <Icon className="h-4 w-4 shrink-0" />
            {item.label}
          </Link>
        );
      })}
    </div>
  );
}

function Sidebar({
  pathname,
  email,
  role,
  onNavigate,
}: {
  pathname: string;
  email: string;
  role?: string;
  onNavigate?: () => void;
}) {
  return (
    <div className="flex h-full flex-col">
      <div className="flex items-center gap-2.5 px-4 py-4">
        <div className="flex h-7 w-7 items-center justify-center rounded-md bg-white text-[11px] font-semibold tracking-tight text-zinc-950">
          H
        </div>
        <div className="min-w-0">
          <p className="truncate text-sm font-semibold tracking-tight text-white">HINVR</p>
          <p className="text-[11px] text-zinc-500">Admin</p>
        </div>
      </div>
      <nav className="flex-1 space-y-5 overflow-y-auto px-3 pb-4">
        <div>
          <p className="mb-1.5 px-2.5 text-[10px] font-medium tracking-[0.14em] text-zinc-500 uppercase">Catalog</p>
          <NavList pathname={pathname} items={catalog} onNavigate={onNavigate} />
        </div>
        <div>
          <p className="mb-1.5 px-2.5 text-[10px] font-medium tracking-[0.14em] text-zinc-500 uppercase">Audience</p>
          <NavList pathname={pathname} items={audience} onNavigate={onNavigate} />
        </div>
        <div>
          <p className="mb-1.5 px-2.5 text-[10px] font-medium tracking-[0.14em] text-zinc-500 uppercase">Workspace</p>
          <NavList pathname={pathname} items={workspace} onNavigate={onNavigate} />
        </div>
      </nav>
      <div className="border-t border-white/10 px-4 py-3">
        <p className="truncate text-xs text-zinc-300">{email}</p>
        <div className="mt-1 flex items-center justify-between text-[11px] text-zinc-500">
          <span className="capitalize">{role || "staff"}</span>
          <form action="/auth/signout" method="post">
            <button className="text-zinc-400 hover:text-white">Sign out</button>
          </form>
        </div>
      </div>
    </div>
  );
}

export function DeskShell({
  email,
  role,
  children,
}: {
  email: string;
  role?: string;
  children: React.ReactNode;
}) {
  const pathname = usePathname();
  const [open, setOpen] = useState(false);
  const trail = crumbs(pathname);

  return (
    <div className="min-h-full bg-zinc-100 lg:flex">
      <aside className="hidden w-60 shrink-0 bg-sidebar text-white lg:block">
        <div className="sticky top-0 h-screen">
          <Sidebar pathname={pathname} email={email} role={role} />
        </div>
      </aside>
      {open ? (
        <div className="fixed inset-0 z-40 lg:hidden">
          <button className="absolute inset-0 bg-black/50" onClick={() => setOpen(false)} aria-label="Close menu" />
          <aside className="relative h-full w-64 bg-sidebar text-white">
            <button
              type="button"
              onClick={() => setOpen(false)}
              className="absolute top-3 right-3 rounded-md p-1 text-zinc-400 hover:bg-white/10 hover:text-white"
              aria-label="Close"
            >
              <IconClose className="h-4 w-4" />
            </button>
            <Sidebar pathname={pathname} email={email} role={role} onNavigate={() => setOpen(false)} />
          </aside>
        </div>
      ) : null}
      <div className="min-w-0 flex-1">
        <header className="sticky top-0 z-20 flex h-12 items-center justify-between border-b border-zinc-200 bg-white/90 px-4 backdrop-blur lg:px-8">
          <div className="flex items-center gap-3">
            <button
              type="button"
              className="rounded-md p-1 text-zinc-600 hover:bg-zinc-100 lg:hidden"
              onClick={() => setOpen(true)}
              aria-label="Open menu"
            >
              <IconMenu className="h-4 w-4" />
            </button>
            <p className="text-[13px] text-zinc-500">
              {trail.map((part, index) => (
                <span key={`${part}-${index}`}>
                  {index > 0 ? <span className="mx-1.5 text-zinc-300">/</span> : null}
                  <span className={index === trail.length - 1 ? "font-medium text-zinc-900" : undefined}>{part}</span>
                </span>
              ))}
            </p>
          </div>
          <span className="rounded-md bg-zinc-100 px-2 py-0.5 text-[11px] font-medium text-zinc-600">Production</span>
        </header>
        <main className="mx-auto w-full max-w-6xl px-4 py-6 lg:px-8 lg:py-8">{children}</main>
      </div>
    </div>
  );
}

export function GateMessage({
  title,
  body,
}: {
  title: string;
  body: string;
}) {
  return (
    <div className="mx-auto flex min-h-full max-w-md flex-col justify-center px-6 py-24">
      <p className="text-[11px] font-medium tracking-[0.16em] text-zinc-500 uppercase">HINVR Admin</p>
      <h1 className="mt-3 text-2xl font-semibold tracking-tight">{title}</h1>
      <p className="mt-3 text-sm leading-6 text-zinc-500">{body}</p>
    </div>
  );
}
