package com.memories.platform.ratelimit.service;

import com.memories.platform.ratelimit.constants.RateLimitScope;
import com.memories.platform.ratelimit.repository.RateLimitBucketRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

@Service
public class RateLimitService {

    private final RateLimitBucketRepository bucketRepository;
    private final Clock clock;

    public RateLimitService(RateLimitBucketRepository bucketRepository, Clock clock) {
        this.bucketRepository = bucketRepository;
        this.clock = clock;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean tryAcquire(
            RateLimitScope scope,
            String subjectKey,
            int maximumRequests,
            Duration window
    ) {
        Instant now = clock.instant();
        bucketRepository.initialize(scope.name(), subjectKey, now);
        return bucketRepository.findForUpdate(scope, subjectKey)
                .orElseThrow(() -> new IllegalStateException("Rate limit bucket was not initialized"))
                .tryAcquire(now, maximumRequests, window);
    }
}
