import { forwardBackendRequest } from "@/lib/backend-api";

type TemplateRouteContext = {
  params: Promise<{ segments: string[] }>;
};

export async function POST(request: Request, context: TemplateRouteContext) {
  return forwardBackendRequest(request, await backendPath(context));
}

export async function PUT(request: Request, context: TemplateRouteContext) {
  return forwardBackendRequest(request, await backendPath(context));
}

async function backendPath(context: TemplateRouteContext) {
  const { segments } = await context.params;
  const safeSegments = segments.map((segment) => encodeURIComponent(segment));
  return `/api/v1/admin/templates/${safeSegments.join("/")}`;
}
