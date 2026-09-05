import type { Metadata } from "next";
import type { ReactNode } from "react";

import AuthProvider from "@/components/auth-provider";
import SiteHeader from "@/components/site-header";

import "./globals.css";
import "./creator.css";

export const metadata: Metadata = {
  title: "Nền tảng thiệp và kỷ niệm",
  description: "Tạo thiệp online và lưu giữ những kỷ niệm quan trọng.",
};

export default function RootLayout({ children }: Readonly<{ children: ReactNode }>) {
  return (
    <html lang="vi">
      <body>
        <AuthProvider>
          <SiteHeader />
          {children}
        </AuthProvider>
      </body>
    </html>
  );
}
