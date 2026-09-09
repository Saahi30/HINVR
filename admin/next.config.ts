import type { NextConfig } from "next";
import path from "node:path";
import { fileURLToPath } from "node:url";

const adminRoot = path.dirname(fileURLToPath(import.meta.url));

const nextConfig: NextConfig = {
  reactCompiler: true,
  agentRules: false,
  turbopack: {
    root: adminRoot,
  },
};

export default nextConfig;
