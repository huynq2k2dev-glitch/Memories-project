package com.memories.platform.ratelimit.entity;

import com.memories.platform.ratelimit.constants.RateLimitScope;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import java.time.Duration;
import java.time.Instant;

@Entity
@Table(name = "rate_limit_buckets")
@IdClass(RateLimitBucketId.class)
public class RateLimitBucket {

    @Id
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30, updatable = false)
    private RateLimitScope scope;

    @Id
    @Column(name = "subject_key", nullable = false, length = 128, updatable = false)
    private String subjectKey;

    @Column(name = "window_started_at", nullable = false)
    private Instant windowStartedAt;

    @Column(name = "request_count", nullable = false)
    private int requestCount;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected RateLimitBucket() {
    }

    public boolean tryAcquire(Instant now, int maximumRequests, Duration window) {
        if (!windowStartedAt.plus(window).isAfter(now)) {
            windowStartedAt = now;
            requestCount = 0;
        }
        updatedAt = now;
        if (requestCount >= maximumRequests) {
            return false;
        }
        requestCount++;
        return true;
    }
}
