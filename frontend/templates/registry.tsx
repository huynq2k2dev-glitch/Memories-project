import type { ComponentType } from "react";

import MemoriesBasicRenderer from "./memories-basic-v1";

export type TemplateRendererProps = {
  payload: MemoryRenderPayload;
};

export type RenderMedia = {
  id: string;
  mimeType: string;
  fileSize: number;
  deliveryUrl: string;
};

export type MemoryRenderPayload = {
  slug: string;
  title: string;
  memoryType: string;
  status: "DRAFT" | "PUBLISHED" | "ARCHIVED";
  visibility: "PRIVATE" | "UNLISTED" | "PUBLIC" | "PASSWORD_PROTECTED";
  summary: string | null;
  themeConfig: Record<string, unknown>;
  eventStartAt: string | null;
  publishedAt: string | null;
  expiresAt: string | null;
  templateVersionId: string;
  componentKey: string;
  rendererVersion: string;
  cover: RenderMedia | null;
  members: Array<{
    id: string;
    roleCode: string;
    fullName: string;
    displayName: string | null;
    description: string | null;
    avatar: RenderMedia | null;
    sortOrder: number;
  }>;
  sections: Array<{
    id: string;
    sectionKey: string;
    sectionType: string;
    title: string | null;
    contentText: string | null;
    config: Record<string, unknown>;
    sortOrder: number;
    required: boolean;
    contentComplete: boolean;
  }>;
  locations: Array<{
    id: string;
    name: string;
    address: string | null;
    latitude: number | null;
    longitude: number | null;
    mapUrl: string | null;
    sortOrder: number;
  }>;
  events: Array<{
    id: string;
    locationId: string | null;
    eventType: string;
    title: string;
    description: string | null;
    startAt: string;
    endAt: string | null;
    timezone: string;
    sortOrder: number;
    rsvpEnabled: boolean;
  }>;
  images: Array<{
    id: string;
    sectionId: string | null;
    caption: string | null;
    altText: string | null;
    sortOrder: number;
    coverCandidate: boolean;
    asset: RenderMedia;
  }>;
};

type RegisteredRenderer = {
  rendererVersion: string;
  component: ComponentType<TemplateRendererProps>;
};

export const TEMPLATE_RENDERER_REGISTRY: Readonly<
  Record<string, RegisteredRenderer>
> = {
  "memories-basic-v1": {
    rendererVersion: "1",
    component: MemoriesBasicRenderer,
  },
};

export function supportsTemplateRenderer(
  componentKey: string,
  rendererVersion: string,
) {
  const registered = TEMPLATE_RENDERER_REGISTRY[componentKey];
  return registered?.rendererVersion === rendererVersion;
}

export function RegisteredTemplateRenderer({
  componentKey,
  rendererVersion,
  payload,
}: TemplateRendererProps & {
  componentKey: string;
  rendererVersion: string;
}) {
  const registered = TEMPLATE_RENDERER_REGISTRY[componentKey];
  if (!registered || registered.rendererVersion !== rendererVersion) {
    return null;
  }
  const Renderer = registered.component;
  return <Renderer payload={payload} />;
}
