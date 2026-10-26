import type { NextConfig } from "next";
import path from "node:path";
import { fileURLToPath } from "node:url";

const adminRoot = path.dirname(fileURLToPath(import.meta.url));
// The repo root, so the desk can import the edge functions' pass-token module
// (supabase/functions/_shared/pass-token.ts) instead of keeping a copy.
const repoRoot = path.join(adminRoot, "..");

const nextConfig: NextConfig = {
  reactCompiler: true,
  agentRules: false,
  turbopack: {
    root: repoRoot,
  },
  outputFileTracingRoot: repoRoot,
};

export default nextConfig;
