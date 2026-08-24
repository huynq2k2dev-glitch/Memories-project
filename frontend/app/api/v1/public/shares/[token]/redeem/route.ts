import { forwardBackendRequest } from "@/lib/backend-api";

export async function POST(
  request: Request,
  { params }: { params: Promise<{ token: string }> },
) {
  const { token } = await params;
  return forwardBackendRequest(
    request,
    `/api/v1/public/shares/${encodeURIComponent(token)}/redeem`,
  );
}
