package com.memories.platform.memory.dto;

import java.util.UUID;

public record GuestMessageSubmissionContextResponse(
        UUID memoryId,
        boolean moderationEnabled
) {
}
