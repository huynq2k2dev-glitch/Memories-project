package com.memories.platform.memory.dto;

import com.memories.platform.memory.constants.MemoryAccessConstants;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UnlockMemoryRequest(
        @NotBlank @Size(max = MemoryAccessConstants.MAX_PASSWORD_LENGTH) String password
) {
    @Override
    public String toString() {
        return "UnlockMemoryRequest[password=[REDACTED]]";
    }
}
