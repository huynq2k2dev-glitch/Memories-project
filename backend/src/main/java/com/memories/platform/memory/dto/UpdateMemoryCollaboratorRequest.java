package com.memories.platform.memory.dto;

import com.memories.platform.memory.entity.MemoryCollaboratorPermission;
import jakarta.validation.constraints.NotNull;

public record UpdateMemoryCollaboratorRequest(
        @NotNull MemoryCollaboratorPermission permission
) {
}
