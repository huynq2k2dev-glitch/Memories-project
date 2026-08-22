import { forwardBackendRequest } from "@/lib/backend-api";

export async function POST(request: Request) {
  return forwardBackendRequest(request, "/api/v1/memories");
}
