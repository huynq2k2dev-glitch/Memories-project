const BACKEND_URL = process.env.BACKEND_URL ?? "http://127.0.0.1:8080";

export async function GET(
  request: Request,
  { params }: { params: Promise<{ assetId: string }> },
) {
  const { assetId } = await params;
  try {
    const backendResponse = await fetch(
      `${BACKEND_URL}/api/v1/public/media/${encodeURIComponent(assetId)}`,
      {
        headers: forwardedHeaders(request),
        redirect: "manual",
        cache: "no-store",
      },
    );
    const location = backendResponse.headers.get("Location");
    if (
      backendResponse.status >= 300 &&
      backendResponse.status < 400 &&
      location
    ) {
      return new Response(null, {
        status: backendResponse.status,
        headers: {
          Location: location,
          "Cache-Control": "no-store",
        },
      });
    }
    return new Response(await backendResponse.text(), {
      status: backendResponse.status,
      headers: {
        "Content-Type":
          backendResponse.headers.get("Content-Type") ?? "application/json",
        "Cache-Control": "no-store",
      },
    });
  } catch {
    return Response.json(
      { code: "BACKEND_UNAVAILABLE", detail: "Dịch vụ tạm thời không khả dụng." },
      { status: 503 },
    );
  }
}

function forwardedHeaders(request: Request) {
  const correlationId = request.headers.get("X-Correlation-Id");
  return correlationId ? { "X-Correlation-Id": correlationId } : undefined;
}
