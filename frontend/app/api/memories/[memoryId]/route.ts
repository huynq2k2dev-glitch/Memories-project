import { forwardBackendRequest } from "@/lib/backend-api";

type MemoryRouteContext = {
  params: Promise<{ memoryId: string }>;
};

export async function GET(request: Request, context: MemoryRouteContext) {
  const { memoryId } = await context.params;
  return forwardBackendRequest(
    request,
    `/api/v1/memories/${encodeURIComponent(memoryId)}`,
  );
}

export async function PUT(request: Request, context: MemoryRouteContext) {
  const { memoryId } = await context.params;
  return forwardBackendRequest(
    request,
    `/api/v1/memories/${encodeURIComponent(memoryId)}`,
  );
}
