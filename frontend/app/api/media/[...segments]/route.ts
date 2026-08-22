import { forwardBackendRequest } from "@/lib/backend-api";

type MediaRouteContext = {
  params: Promise<{ segments: string[] }>;
};

async function forward(request: Request, context: MediaRouteContext) {
  const { segments } = await context.params;
  const suffix = segments.map(encodeURIComponent).join("/");
  const query = new URL(request.url).search;
  return forwardBackendRequest(request, `/api/v1/media/${suffix}${query}`);
}

export async function POST(request: Request, context: MediaRouteContext) {
  return forward(request, context);
}

export async function DELETE(request: Request, context: MediaRouteContext) {
  return forward(request, context);
}
