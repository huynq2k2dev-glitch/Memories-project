package com.memories.platform.memory.dto;

public record IssuedShareLinkResponse(
        ShareLinkResponse shareLink,
        String accessToken,
        String sharePath
) {
}
