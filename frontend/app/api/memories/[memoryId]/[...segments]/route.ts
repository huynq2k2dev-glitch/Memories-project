import { forwardBackendRequest } from "@/lib/backend-api";

type MemoryContentRouteContext = {
  params: Promise<{ memoryId: string; segments: string[] }>;
};

async function forward(request: Request, context: MemoryContentRouteContext) {
  const { memoryId, segments } = await context.params;
  const suffix = segments.map(encodeURIComponent).join("/");
  const query = new URL(request.url).search;
  return forwardBackendRequest(
    request,
    `/api/v1/memories/${encodeURIComponent(memoryId)}/${suffix}${query}`,
  );
}

export async function GET(request: Request, context: MemoryContentRouteContext) {
  return forward(request, context);
}

export async function POST(request: Request, context: MemoryContentRouteContext) {
  return forward(request, context);
}

export async function PUT(request: Request, context: MemoryContentRouteContext) {
  return forward(request, context);
}

export async function DELETE(
  request: Request,
  context: MemoryContentRouteContext,
) {
  return forward(request, context);
}
