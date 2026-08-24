package com.memories.platform.ratelimit.service;

import com.memories.platform.config.RateLimitProperties;
import com.memories.platform.ratelimit.repository.RateLimitBucketRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

@Service
public class RateLimitRetentionService {

    private final RateLimitBucketRepository bucketRepository;
    private final RateLimitProperties properties;
    private final Clock clock;

    public RateLimitRetentionService(
            RateLimitBucketRepository bucketRepository,
            RateLimitProperties properties,
            Clock clock
    ) {
        this.bucketRepository = bucketRepository;
        this.properties = properties;
        this.clock = clock;
    }

    @Scheduled(cron = "${platform.rate-limit.bucket-purge-cron}", zone = "UTC")
    @Transactional
    public void deleteStaleBuckets() {
        bucketRepository.deleteStale(clock.instant().minus(properties.bucketRetention()));
    }
}
