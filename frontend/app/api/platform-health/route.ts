const DEFAULT_BACKEND_URL = "http://127.0.0.1:8080";

export async function GET(request: Request) {
  const backendUrl = process.env.BACKEND_URL ?? DEFAULT_BACKEND_URL;
  const correlationId = request.headers.get("X-Correlation-Id") ?? crypto.randomUUID();

  try {
    const response = await fetch(`${backendUrl}/api/v1/health`, {
      cache: "no-store",
      headers: {
        "X-Correlation-Id": correlationId,
      },
    });
    const payload = await response.text();

    return new Response(payload, {
      status: response.status,
      headers: {
        "Content-Type": response.headers.get("Content-Type") ?? "application/json",
        "X-Correlation-Id": response.headers.get("X-Correlation-Id") ?? correlationId,
      },
    });
  } catch {
    return Response.json(
      { status: "DOWN", database: "UNKNOWN" },
      {
        status: 503,
        headers: { "X-Correlation-Id": correlationId },
      },
    );
  }
}
