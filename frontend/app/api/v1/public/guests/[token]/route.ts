import { forwardBackendRequest } from "@/lib/backend-api";

export async function GET(
  request: Request,
  { params }: { params: Promise<{ token: string }> },
) {
  const { token } = await params;
  return forwardBackendRequest(
    request,
    `/api/v1/public/guests/${encodeURIComponent(token)}`,
  );
}
