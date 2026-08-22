"use client";

import { useEffect, useState } from "react";

type PlatformHealth = {
  status: string;
  database: string;
};

const INITIAL_HEALTH: PlatformHealth = {
  status: "CHECKING",
  database: "CHECKING",
};

export default function Home() {
  const [health, setHealth] = useState<PlatformHealth>(INITIAL_HEALTH);

  useEffect(() => {
    const controller = new AbortController();

    async function loadHealth() {
      try {
        const response = await fetch("/api/platform-health", {
          cache: "no-store",
          signal: controller.signal,
        });
        const payload = (await response.json()) as PlatformHealth;
        setHealth(payload);
      } catch (error) {
        if (!(error instanceof DOMException && error.name === "AbortError")) {
          setHealth({ status: "DOWN", database: "UNKNOWN" });
        }
      }
    }

    void loadHealth();

    return () => controller.abort();
  }, []);

  return (
    <main>
      <section className="hero" aria-labelledby="platform-title">
        <p className="eyebrow">Foundation</p>
        <h1 id="platform-title">Nền tảng thiệp và kỷ niệm</h1>
        <p className="summary">
          Không gian để tạo thiệp online và lưu giữ những dấu mốc quan trọng.
        </p>
        <div className="health-card" aria-live="polite">
          <StatusRow label="Backend" status={health.status} />
          <StatusRow label="PostgreSQL" status={health.database} />
        </div>
      </section>
    </main>
  );
}

function StatusRow({ label, status }: { label: string; status: string }) {
  const displayStatus = status === "UP" ? "Hoạt động" : status === "CHECKING" ? "Đang kiểm tra" : "Gián đoạn";

  return (
    <p>
      <span>{label}:</span> <strong>{displayStatus}</strong>
    </p>
  );
}
