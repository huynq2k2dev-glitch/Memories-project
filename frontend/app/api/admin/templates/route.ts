import { forwardBackendRequest } from "@/lib/backend-api";

const BACKEND_PATH = "/api/v1/admin/templates";

export async function GET(request: Request) {
  return forwardBackendRequest(request, `${BACKEND_PATH}${new URL(request.url).search}`);
}

export async function POST(request: Request) {
  return forwardBackendRequest(request, BACKEND_PATH);
}
