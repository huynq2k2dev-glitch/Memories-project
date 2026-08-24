package com.memories.platform.memory.repository;

import com.memories.platform.memory.entity.MemoryAccessGrant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.UUID;

public interface MemoryAccessGrantRepository extends JpaRepository<MemoryAccessGrant, UUID> {

    boolean existsByMemoryIdAndTokenHashAndExpiresAtAfter(
            UUID memoryId,
            String tokenHash,
            Instant now
    );

    void deleteAllByMemoryId(UUID memoryId);

    void deleteByExpiresAtLessThanEqual(Instant now);
}
