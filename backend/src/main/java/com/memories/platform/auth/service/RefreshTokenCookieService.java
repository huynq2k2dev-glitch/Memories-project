package com.memories.platform.auth.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

@Service
public class RefreshTokenCookieService {

    private static final String COOKIE_PATH = "/api/auth";

    private final Clock clock;
    private final String cookieName;
    private final boolean secure;

    public RefreshTokenCookieService(
            Clock clock,
            @Value("${platform.auth.refresh-cookie-name}") String cookieName,
            @Value("${platform.auth.refresh-cookie-secure}") boolean secure
    ) {
        this.clock = clock;
        this.cookieName = cookieName;
        this.secure = secure;
    }

    public String create(String rawToken, Instant expiresAt) {
        Duration remainingLifetime = Duration.between(clock.instant(), expiresAt);
        if (remainingLifetime.isNegative()) {
            remainingLifetime = Duration.ZERO;
        }
        return baseCookie(rawToken)
                .maxAge(remainingLifetime)
                .build()
                .toString();
    }

    public String clear() {
        return baseCookie("")
                .maxAge(Duration.ZERO)
                .build()
                .toString();
    }

    private ResponseCookie.ResponseCookieBuilder baseCookie(String value) {
        return ResponseCookie.from(cookieName, value)
                .httpOnly(true)
                .secure(secure)
                .sameSite("Strict")
                .path(COOKIE_PATH);
    }
}
