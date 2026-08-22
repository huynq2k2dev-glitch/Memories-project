package com.memories.platform.config;

import com.memories.platform.media.entity.MediaStorageProvider;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;
import java.time.Duration;

@ConfigurationProperties(prefix = "platform.media")
public record MediaStorageProperties(
        MediaStorageProvider provider,
        String bucket,
        String region,
        URI internalEndpoint,
        URI publicEndpoint,
        String accessKey,
        String secretKey,
        Duration presignedPutTtl,
        Duration presignedGetTtl,
        long maxFileSize,
        long maxAssetsPerOwner,
        long maxBytesPerOwner
) {
}
