package com.memories.platform.memory.dto;

import java.util.UUID;

public record GuestMessageModeratorContextResponse(
        UUID memoryId,
        UUID actorId
) {
}
