package com.memories.platform.memory.service;

import com.memories.platform.memory.constants.MemoryAccessConstants;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Service
public class MemoryAccessCookieService {

    private final Clock clock;
    private final boolean secure;

    public MemoryAccessCookieService(
            Clock clock,
            @Value("${platform.memory.password-cookie-secure}") boolean secure
    ) {
        this.clock = clock;
        this.secure = secure;
    }

    public String create(UUID memoryId, String rawToken, Instant expiresAt) {
        Duration remainingLifetime = Duration.between(clock.instant(), expiresAt);
        if (remainingLifetime.isNegative()) {
            remainingLifetime = Duration.ZERO;
        }
        return ResponseCookie.from(cookieName(memoryId), rawToken)
                .httpOnly(true)
                .secure(secure)
                .sameSite(MemoryAccessConstants.COOKIE_SAME_SITE)
                .path(MemoryAccessConstants.COOKIE_PATH)
                .maxAge(remainingLifetime)
                .build()
                .toString();
    }

    public String createSession(UUID memoryId, String rawToken) {
        return ResponseCookie.from(cookieName(memoryId), rawToken)
                .httpOnly(true)
                .secure(secure)
                .sameSite(MemoryAccessConstants.COOKIE_SAME_SITE)
                .path(MemoryAccessConstants.COOKIE_PATH)
                .build()
                .toString();
    }

    public String token(Map<String, String> cookies, UUID memoryId) {
        return cookies.get(cookieName(memoryId));
    }

    private String cookieName(UUID memoryId) {
        return MemoryAccessConstants.COOKIE_NAME_PREFIX + memoryId;
    }
}
