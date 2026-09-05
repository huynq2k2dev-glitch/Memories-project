import AdminTemplateClient from "./admin-template-client";
import RequireAuth from "@/components/require-auth";

export default function AdminTemplatesPage() {
  return <RequireAuth><AdminTemplateClient /></RequireAuth>;
}
