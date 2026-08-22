const BACKEND_URL = process.env.BACKEND_URL ?? "http://127.0.0.1:8080";
const REFRESH_COOKIE_NAME = "memories_refresh";

export async function forwardJsonPost(
  request: Request,
  path: string,
): Promise<Response> {
  return forwardBackendRequest(request, path);
}

export async function forwardBackendRequest(
  request: Request,
  path: string,
): Promise<Response> {
  try {
    const correlationId = request.headers.get("X-Correlation-Id");
    const authorization = request.headers.get("Authorization");
    const cookie = shouldForwardRefreshCookie(path)
      ? selectRefreshCookie(request.headers.get("Cookie"))
      : null;
    const hasRequestBody = request.method !== "GET" && request.method !== "HEAD";
    const backendResponse = await fetch(`${BACKEND_URL}${path}`, {
      method: request.method,
      headers: {
        ...(hasRequestBody ? { "Content-Type": "application/json" } : {}),
        ...(correlationId ? { "X-Correlation-Id": correlationId } : {}),
        ...(authorization ? { Authorization: authorization } : {}),
        ...(cookie ? { Cookie: cookie } : {}),
      },
      ...(hasRequestBody ? { body: await request.text() } : {}),
      cache: "no-store",
    });
    const responseHeaders = new Headers({
      "Content-Type": backendResponse.headers.get("Content-Type") ?? "application/json",
      "Cache-Control": "no-store",
      Pragma: "no-cache",
    });
    const responseCorrelationId = backendResponse.headers.get("X-Correlation-Id");
    if (responseCorrelationId) {
      responseHeaders.set("X-Correlation-Id", responseCorrelationId);
    }
    const setCookie = backendResponse.headers.get("Set-Cookie");
    if (setCookie) {
      responseHeaders.set("Set-Cookie", setCookie);
    }
    const hasNoResponseBody = [204, 205, 304].includes(backendResponse.status);
    const responseBody = hasNoResponseBody ? null : await backendResponse.text();
    return new Response(responseBody, {
      status: backendResponse.status,
      headers: responseHeaders,
    });
  } catch {
    return Response.json(
      { code: "BACKEND_UNAVAILABLE", detail: "Dịch vụ tạm thời không khả dụng." },
      { status: 503 },
    );
  }
}

function shouldForwardRefreshCookie(path: string) {
  return path === "/api/v1/auth/refresh" || path === "/api/v1/auth/logout";
}

function selectRefreshCookie(cookieHeader: string | null) {
  if (!cookieHeader) {
    return null;
  }
  return (
    cookieHeader
      .split(";")
      .map((cookie) => cookie.trim())
      .find((cookie) => cookie.startsWith(`${REFRESH_COOKIE_NAME}=`)) ?? null
  );
}
