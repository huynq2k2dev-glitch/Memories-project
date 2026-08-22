"use client";

let accessToken: string | null = null;
let pendingRefresh: Promise<boolean> | null = null;

export function storeAccessToken(token: string) {
  accessToken = token;
}

export function clearAccessToken() {
  accessToken = null;
}

export async function authenticatedFetch(
  path: string,
  init: RequestInit = {},
) {
  if (!path.startsWith("/api/")) {
    throw new Error("Authenticated requests must target an internal API route");
  }
  const response = await fetch(path, withAccessToken(init));
  if (response.status !== 401 || !(await refreshAccessToken())) {
    return response;
  }
  return fetch(path, withAccessToken(init));
}

export async function logoutCurrentSession() {
  try {
    await fetch("/api/auth/logout", { method: "POST" });
  } finally {
    clearAccessToken();
  }
}

export async function logoutAllSessions() {
  try {
    await authenticatedFetch("/api/auth/logout-all", { method: "POST" });
  } finally {
    clearAccessToken();
  }
}

function withAccessToken(init: RequestInit) {
  const headers = new Headers(init.headers);
  if (accessToken) {
    headers.set("Authorization", `Bearer ${accessToken}`);
  }
  return { ...init, headers };
}

async function refreshAccessToken() {
  if (!pendingRefresh) {
    pendingRefresh = requestRefresh().finally(() => {
      pendingRefresh = null;
    });
  }
  return pendingRefresh;
}

async function requestRefresh() {
  try {
    const response = await fetch("/api/auth/refresh", { method: "POST" });
    if (!response.ok) {
      clearAccessToken();
      return false;
    }
    const payload = (await response.json()) as { accessToken: string };
    storeAccessToken(payload.accessToken);
    return true;
  } catch {
    clearAccessToken();
    return false;
  }
}
