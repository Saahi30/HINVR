import type { Metadata } from "next";
import { Newsreader, Outfit } from "next/font/google";
import "./globals.css";
import { SITE_URL } from "@/lib/site";

const newsreader = Newsreader({
  variable: "--font-newsreader",
  subsets: ["latin"],
});

const outfit = Outfit({
  variable: "--font-outfit",
  subsets: ["latin"],
});

const description =
  "HINVR membership plans: official live darshan, a digital temple pass, Priority Assist at partner mandirs, and a person at the desk for your family.";

export const metadata: Metadata = {
  metadataBase: new URL(SITE_URL),
  title: "HINVR Membership — plans and pricing",
  description,
  alternates: { canonical: "/" },
  openGraph: {
    type: "website",
    url: "/",
    siteName: "HINVR",
    title: "HINVR Membership",
    description,
    locale: "en_IN",
  },
  twitter: {
    card: "summary",
    title: "HINVR Membership",
    description,
  },
};

export default function RootLayout({ children }: Readonly<{ children: React.ReactNode }>) {
  return (
    <html lang="en" className={`${newsreader.variable} ${outfit.variable} h-full antialiased`}>
      <body className="min-h-full bg-linen font-sans text-ink">{children}</body>
    </html>
  );
}
