package com.memories.platform.memory.dto;

import com.memories.platform.memory.entity.MemoryCollaboratorPermission;
import com.memories.platform.memory.entity.MemoryCollaboratorStatus;

import java.time.Instant;
import java.util.UUID;

public record MemoryCollaboratorResponse(
        UUID id,
        UUID userId,
        String displayName,
        boolean accountActive,
        MemoryCollaboratorPermission permission,
        MemoryCollaboratorStatus status,
        UUID invitedBy,
        Instant createdAt,
        Instant updatedAt,
        Instant revokedAt
) {
}
