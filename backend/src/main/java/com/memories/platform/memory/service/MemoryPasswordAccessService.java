package com.memories.platform.memory.service;

import com.memories.platform.memory.constants.MemoryAccessConstants;
import com.memories.platform.memory.constants.MemoryPublishingConstants;
import com.memories.platform.memory.entity.Memory;
import com.memories.platform.memory.entity.MemoryAccessGrant;
import com.memories.platform.memory.entity.MemoryStatus;
import com.memories.platform.memory.entity.MemoryVisibility;
import com.memories.platform.memory.exception.InvalidMemoryAccessPolicyException;
import com.memories.platform.memory.exception.MemoryNotFoundException;
import com.memories.platform.memory.exception.MemoryPasswordRequiredException;
import com.memories.platform.memory.repository.MemoryAccessGrantRepository;
import com.memories.platform.memory.repository.MemoryRepository;
import com.memories.platform.utils.TokenHashUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

@Service
public class MemoryPasswordAccessService {

    private final MemoryRepository memoryRepository;
    private final MemoryAccessGrantRepository grantRepository;
    private final MemoryAccessCookieService cookieService;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom secureRandom;
    private final Clock clock;
    private final Duration grantTtl;

    public MemoryPasswordAccessService(
            MemoryRepository memoryRepository,
            MemoryAccessGrantRepository grantRepository,
            MemoryAccessCookieService cookieService,
            PasswordEncoder passwordEncoder,
            SecureRandom secureRandom,
            Clock clock,
            @Value("${platform.memory.password-access-ttl}") Duration grantTtl
    ) {
        this.memoryRepository = memoryRepository;
        this.grantRepository = grantRepository;
        this.cookieService = cookieService;
        this.passwordEncoder = passwordEncoder;
        this.secureRandom = secureRandom;
        this.clock = clock;
        this.grantTtl = grantTtl;
    }

    @Transactional
    public String unlock(String slug, String password) {
        Instant now = clock.instant();
        Memory memory = memoryRepository.findPublicBySlug(
                slug,
                MemoryStatus.PUBLISHED,
                MemoryPublishingConstants.PUBLISHABLE_VISIBILITIES,
                now
        ).filter(candidate -> candidate.getVisibility() == MemoryVisibility.PASSWORD_PROTECTED)
                .orElseThrow(MemoryNotFoundException::new);

        if (!passwordEncoder.matches(password, memory.getAccessPasswordHash())) {
            throw new MemoryPasswordRequiredException();
        }

        grantRepository.deleteByExpiresAtLessThanEqual(now);
        String rawToken = generateToken();
        Instant expiresAt = now.plus(grantTtl);
        grantRepository.save(new MemoryAccessGrant(
                UUID.randomUUID(),
                memory.getId(),
                TokenHashUtils.sha256(rawToken),
                expiresAt,
                now
        ));
        return cookieService.create(memory.getId(), rawToken, expiresAt);
    }

    @Transactional(readOnly = true)
    public boolean hasValidGrant(UUID memoryId, Map<String, String> cookies) {
        String rawToken = cookieService.token(cookies, memoryId);
        return rawToken != null && grantRepository.existsByMemoryIdAndTokenHashAndExpiresAtAfter(
                memoryId,
                TokenHashUtils.sha256(rawToken),
                clock.instant()
        );
    }

    public String passwordHashForUpdate(
            Memory memory,
            MemoryVisibility visibility,
            String password
    ) {
        boolean passwordProvided = password != null && !password.isBlank();
        if (visibility == MemoryVisibility.PASSWORD_PROTECTED) {
            if (passwordProvided) {
                return passwordEncoder.encode(password);
            }
            if (memory.getVisibility() == MemoryVisibility.PASSWORD_PROTECTED
                    && memory.getAccessPasswordHash() != null) {
                return memory.getAccessPasswordHash();
            }
            throw new InvalidMemoryAccessPolicyException();
        }
        if (passwordProvided) {
            throw new InvalidMemoryAccessPolicyException();
        }
        return null;
    }

    @Transactional
    public void revokeAll(UUID memoryId) {
        grantRepository.deleteAllByMemoryId(memoryId);
    }

    private String generateToken() {
        byte[] bytes = new byte[MemoryAccessConstants.ACCESS_TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
