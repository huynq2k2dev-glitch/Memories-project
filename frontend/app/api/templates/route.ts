import { forwardBackendRequest } from "@/lib/backend-api";

export async function GET(request: Request) {
  const requestUrl = new URL(request.url);
  const query = requestUrl.searchParams.toString();
  const backendPath = `/api/v1/templates${query ? `?${query}` : ""}`;
  return forwardBackendRequest(request, backendPath);
}
