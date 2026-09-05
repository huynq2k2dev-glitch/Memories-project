package com.memories.platform.memory.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.memories.platform.common.domain.MemoryType;
import com.memories.platform.guest.dto.GuestMessagePublicResponse;
import com.memories.platform.template.dto.HtmlBook;
import com.memories.platform.memory.entity.MemoryStatus;
import com.memories.platform.memory.entity.MemoryVisibility;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record MemoryRenderResponse(
        String slug,
        String title,
        MemoryType memoryType,
        MemoryStatus status,
        MemoryVisibility visibility,
        String summary,
        JsonNode themeConfig,
        Instant eventStartAt,
        Instant publishedAt,
        Instant expiresAt,
        UUID templateVersionId,
        String componentKey,
        String rendererVersion,
        RenderMedia cover,
        List<RenderMember> members,
        List<RenderSection> sections,
        List<RenderLocation> locations,
        List<RenderEvent> events,
        List<RenderImage> images,
        List<GuestMessagePublicResponse> messages,
        HtmlBook book
) {

    public record RenderMedia(
            UUID id,
            String mimeType,
            long fileSize,
            String deliveryUrl
    ) {
    }

    public record RenderMember(
            UUID id,
            String roleCode,
            String fullName,
            String displayName,
            String description,
            RenderMedia avatar,
            int sortOrder
    ) {
    }

    public record RenderSection(
            UUID id,
            String sectionKey,
            String sectionType,
            String title,
            String contentText,
            JsonNode config,
            int sortOrder,
            boolean required,
            boolean contentComplete
    ) {
    }

    public record RenderLocation(
            UUID id,
            String name,
            String address,
            BigDecimal latitude,
            BigDecimal longitude,
            String mapUrl,
            int sortOrder
    ) {
    }

    public record RenderEvent(
            UUID id,
            UUID locationId,
            String eventType,
            String title,
            String description,
            Instant startAt,
            Instant endAt,
            String timezone,
            int sortOrder,
            boolean rsvpEnabled
    ) {
    }

    public record RenderImage(
            UUID id,
            UUID sectionId,
            String caption,
            String altText,
            int sortOrder,
            boolean coverCandidate,
            RenderMedia asset
    ) {
    }
}
