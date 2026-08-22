import { forwardJsonPost } from "@/lib/backend-api";

export async function POST(request: Request) {
  return forwardJsonPost(
    request,
    "/api/v1/auth/email-verifications/confirm",
  );
}
