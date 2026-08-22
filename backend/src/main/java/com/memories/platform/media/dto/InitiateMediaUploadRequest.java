package com.memories.platform.media.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record InitiateMediaUploadRequest(
        @NotBlank @Size(max = 255) String originalFileName,
        @NotBlank @Size(max = 100) String mimeType,
        @Min(1) @Max(10_485_760) long fileSize,
        @Size(max = 128) String checksumSha256
) {
}
