import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  allowedDevOrigins: ["127.0.0.1"],
  output: "standalone",
  async headers() {
    return [{
      source: "/memories/:path*",
      headers: [{
        key: "Content-Security-Policy",
        // Next hydration needs inline scripts; template markup is validated separately.
        value: `object-src 'none'; base-uri 'self'; frame-ancestors 'self'; form-action 'self'; script-src 'self' 'unsafe-inline'${process.env.NODE_ENV === "development" ? " 'unsafe-eval'" : ""}; script-src-attr 'none'`,
      }],
    }];
  },
};

export default nextConfig;
