package com.memories.platform.memory.dto;

import com.memories.platform.memory.entity.MemoryCollaboratorPermission;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AddMemoryCollaboratorRequest(
        @NotBlank @Email @Size(max = 320) String email,
        @NotNull MemoryCollaboratorPermission permission
) {
    public AddMemoryCollaboratorRequest {
        email = email == null ? null : email.trim();
    }
}
