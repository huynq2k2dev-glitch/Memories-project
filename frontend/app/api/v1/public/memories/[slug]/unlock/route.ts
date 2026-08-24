import { forwardBackendRequest } from "@/lib/backend-api";

export async function POST(
  request: Request,
  { params }: { params: Promise<{ slug: string }> },
) {
  const { slug } = await params;
  return forwardBackendRequest(
    request,
    `/api/v1/public/memories/${encodeURIComponent(slug)}/unlock`,
  );
}
