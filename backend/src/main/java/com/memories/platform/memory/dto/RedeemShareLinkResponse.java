package com.memories.platform.memory.dto;

import com.memories.platform.memory.entity.ShareLinkPermission;

public record RedeemShareLinkResponse(
        String memoryPath,
        ShareLinkPermission permission
) {
}
