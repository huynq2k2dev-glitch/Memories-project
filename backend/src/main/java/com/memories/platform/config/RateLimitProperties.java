package com.memories.platform.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "platform.rate-limit")
public record RateLimitProperties(
        int loginCount,
        Duration loginWindow,
        int uploadCount,
        Duration uploadWindow,
        String ipHashSecret,
        boolean trustForwardedFor,
        Duration bucketRetention
) {

    public RateLimitProperties {
        if (loginCount < 1 || uploadCount < 1) {
            throw new IllegalArgumentException("Rate limit counts must be positive");
        }
        if (loginWindow == null || loginWindow.isZero() || loginWindow.isNegative()
                || uploadWindow == null || uploadWindow.isZero() || uploadWindow.isNegative()) {
            throw new IllegalArgumentException("Rate limit windows must be positive");
        }
        if (bucketRetention == null || bucketRetention.isZero() || bucketRetention.isNegative()) {
            throw new IllegalArgumentException("Rate limit bucket retention must be positive");
        }
    }
}
