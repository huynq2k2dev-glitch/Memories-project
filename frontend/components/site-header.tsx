"use client";

import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { useEffect, useRef, useState } from "react";

import { useAuth } from "@/components/auth-provider";

export default function SiteHeader() {
  const { account, signOut, status } = useAuth();
  const router = useRouter();
  const pathname = usePathname();
  const [mobileOpen, setMobileOpen] = useState(false);
  const [loggingOut, setLoggingOut] = useState(false);
  const menuButtonRef = useRef<HTMLButtonElement>(null);
  const drawerRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (!mobileOpen) {
      return;
    }
    const drawer = drawerRef.current;
    const focusable = drawer?.querySelectorAll<HTMLElement>(
      'a[href], button:not([disabled]), [tabindex]:not([tabindex="-1"])',
    );
    focusable?.[0]?.focus();

    function handleKeyDown(event: KeyboardEvent) {
      if (event.key === "Escape") {
        setMobileOpen(false);
        menuButtonRef.current?.focus();
        return;
      }
      if (event.key !== "Tab" || !focusable?.length) {
        return;
      }
      const first = focusable[0];
      const last = focusable[focusable.length - 1];
      if (event.shiftKey && document.activeElement === first) {
        event.preventDefault();
        last.focus();
      } else if (!event.shiftKey && document.activeElement === last) {
        event.preventDefault();
        first.focus();
      }
    }

    document.addEventListener("keydown", handleKeyDown);
    return () => document.removeEventListener("keydown", handleKeyDown);
  }, [mobileOpen]);

  async function logout() {
    setLoggingOut(true);
    try {
      await signOut();
      router.replace("/");
    } finally {
      setLoggingOut(false);
      setMobileOpen(false);
    }
  }

  if (pathname === "/admin" || pathname.startsWith("/admin/")) {
    return (
      <header className="administration-header">
        <Link href="/admin/templates" className="brand">Memories <small>Quản trị</small></Link>
        <nav aria-label="Điều hướng quản trị">
          <Link href="/admin/templates">Quản lý mẫu</Link>
          <Link href="/memories">Về khu vực người dùng</Link>
          {status === "authenticated" ? <button type="button" disabled={loggingOut} onClick={() => void logout()}>Đăng xuất</button> : null}
        </nav>
      </header>
    );
  }

  return (
    <header className="site-header">
      <div className="site-header-inner">
        <Link className="brand" href="/" aria-label="Memories - Trang chủ">
          Memories
        </Link>
        <nav className="desktop-nav" aria-label="Điều hướng chính">
          <Navigation status={status} accountName={account?.displayName} logout={logout} loggingOut={loggingOut} onNavigate={() => setMobileOpen(false)} />
        </nav>
        <button
          ref={menuButtonRef}
          className="menu-button"
          type="button"
          aria-label={mobileOpen ? "Đóng menu" : "Mở menu"}
          aria-expanded={mobileOpen}
          aria-controls="mobile-navigation"
          onClick={() => setMobileOpen((open) => !open)}
        >
          <span aria-hidden="true">{mobileOpen ? "×" : "☰"}</span>
        </button>
      </div>
      {mobileOpen ? (
        <div className="mobile-overlay" onMouseDown={() => setMobileOpen(false)}>
          <div
            id="mobile-navigation"
            ref={drawerRef}
            className="mobile-drawer"
            role="dialog"
            aria-modal="true"
            aria-label="Điều hướng"
            onMouseDown={(event) => event.stopPropagation()}
          >
            <Navigation status={status} accountName={account?.displayName} logout={logout} loggingOut={loggingOut} onNavigate={() => setMobileOpen(false)} />
          </div>
        </div>
      ) : null}
    </header>
  );
}

function Navigation({
  status,
  accountName,
  logout,
  loggingOut,
  onNavigate,
}: {
  status: "loading" | "authenticated" | "anonymous";
  accountName: string | undefined;
  logout: () => Promise<void>;
  loggingOut: boolean;
  onNavigate: () => void;
}) {
  if (status === "loading") {
    return <span className="nav-loading">Đang khôi phục phiên…</span>;
  }
  if (status === "anonymous") {
    return (
      <>
        <Link href="/" onClick={onNavigate}>Trang chủ</Link>
        <Link href="/#how-it-works" onClick={onNavigate}>Cách hoạt động</Link>
        <Link href="/login" onClick={onNavigate}>Đăng nhập</Link>
        <Link className="nav-cta" href="/register" onClick={onNavigate}>Đăng ký</Link>
      </>
    );
  }
  return (
    <>
      <Link href="/" onClick={onNavigate}>Trang chủ</Link>
      <Link href="/memories" onClick={onNavigate}>Kỷ niệm của tôi</Link>
      <Link className="nav-cta" href="/memories/new" onClick={onNavigate}>Tạo kỷ niệm</Link>
      <details className="account-menu">
        <summary>{accountName ?? "Tài khoản"}</summary>
        <button type="button" disabled={loggingOut} onClick={() => void logout()}>
          {loggingOut ? "Đang đăng xuất…" : "Đăng xuất"}
        </button>
      </details>
    </>
  );
}
