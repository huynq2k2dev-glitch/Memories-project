"use client";

import { useEffect, useState } from "react";

type RedeemShareLinkResponse = {
  memoryPath: string;
  permission: "VIEW" | "RSVP";
};

export default function ShareLinkRedeemer({
  accessToken,
}: {
  accessToken: string;
}) {
  const [error, setError] = useState("");

  useEffect(() => {
    const controller = new AbortController();
    void fetch(
      `/api/v1/public/shares/${encodeURIComponent(accessToken)}/redeem`,
      { method: "POST", signal: controller.signal },
    )
      .then(async (response) => {
        if (!response.ok) {
          const problem = (await response.json().catch(() => null)) as {
            detail?: string;
          } | null;
          throw new Error(
            problem?.detail ?? "Link chia sẻ không còn khả dụng.",
          );
        }
        return (await response.json()) as RedeemShareLinkResponse;
      })
      .then((result) => {
        window.location.replace(result.memoryPath);
      })
      .catch((reason: unknown) => {
        if (!controller.signal.aborted) {
          setError(
            reason instanceof Error
              ? reason.message
              : "Link chia sẻ không còn khả dụng.",
          );
        }
      });
    return () => controller.abort();
  }, [accessToken]);

  return (
    <main className="public-memory-message">
      <h1>{error ? "Không thể mở link" : "Đang mở memory…"}</h1>
      <p role={error ? "alert" : "status"}>
        {error || "Hệ thống đang xác thực quyền truy cập của bạn."}
      </p>
    </main>
  );
}
