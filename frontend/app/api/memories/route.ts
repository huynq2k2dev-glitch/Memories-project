import { forwardBackendRequest } from "@/lib/backend-api";

export async function GET(request: Request) {
  const query = new URL(request.url).search;
  return forwardBackendRequest(request, `/api/v1/memories${query}`);
}

export async function POST(request: Request) {
  return forwardBackendRequest(request, "/api/v1/memories");
}
