package com.memories.platform.template.dto;

import com.memories.platform.common.domain.MemoryType;
import com.memories.platform.template.entity.TemplateStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AdminTemplateResponse(
        UUID id,
        String code,
        String name,
        MemoryType memoryType,
        String description,
        TemplateStatus status,
        long version,
        Instant createdAt,
        Instant updatedAt,
        List<AdminTemplateVersionResponse> versions
) {
}
