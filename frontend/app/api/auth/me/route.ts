import { forwardBackendRequest } from "@/lib/backend-api";

export async function GET(request: Request) {
  return forwardBackendRequest(request, "/api/v1/auth/me");
}
