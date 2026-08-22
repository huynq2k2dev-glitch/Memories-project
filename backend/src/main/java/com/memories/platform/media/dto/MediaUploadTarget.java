package com.memories.platform.media.dto;

import java.util.Map;

public record MediaUploadTarget(
        String uploadUrl,
        Map<String, String> requiredHeaders
) {
}
