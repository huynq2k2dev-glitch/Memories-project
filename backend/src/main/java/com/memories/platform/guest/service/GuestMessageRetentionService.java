package com.memories.platform.guest.service;

import com.memories.platform.guest.repository.GuestMessageRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;

@Service
public class GuestMessageRetentionService {

    private final GuestMessageRepository messageRepository;
    private final Clock clock;
    private final Duration retention;

    public GuestMessageRetentionService(
            GuestMessageRepository messageRepository,
            Clock clock,
            @Value("${platform.guest-message.ip-hash-retention}") Duration retention
    ) {
        if (retention.isNegative() || retention.isZero()) {
            throw new IllegalArgumentException("Guest message IP hash retention must be positive");
        }
        this.messageRepository = messageRepository;
        this.clock = clock;
        this.retention = retention;
    }

    @Scheduled(
            cron = "${platform.guest-message.ip-hash-purge-cron}",
            zone = "UTC"
    )
    @Transactional
    public void clearExpiredIpHashes() {
        messageRepository.clearExpiredIpHashes(clock.instant().minus(retention));
    }
}
