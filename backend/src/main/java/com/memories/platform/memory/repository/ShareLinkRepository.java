package com.memories.platform.memory.repository;

import com.memories.platform.memory.entity.ShareLink;
import com.memories.platform.memory.entity.ShareLinkStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ShareLinkRepository extends JpaRepository<ShareLink, UUID> {

    List<ShareLink> findAllByMemoryIdOrderByCreatedAtDesc(UUID memoryId);

    @Query("""
            select link
            from ShareLink link
            where link.memoryId = :memoryId
              and link.tokenHash = :tokenHash
              and link.status = :status
              and (link.expiresAt is null or link.expiresAt > :now)
            """)
    Optional<ShareLink> findValidGrant(
            @Param("memoryId") UUID memoryId,
            @Param("tokenHash") String tokenHash,
            @Param("status") ShareLinkStatus status,
            @Param("now") Instant now
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select link
            from ShareLink link
            where link.id = :shareLinkId
              and link.memoryId = :memoryId
            """)
    Optional<ShareLink> findForUpdateByIdAndMemoryId(
            @Param("shareLinkId") UUID shareLinkId,
            @Param("memoryId") UUID memoryId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<ShareLink> findForUpdateByTokenHash(String tokenHash);

    @Modifying
    @Query("""
            update ShareLink link
            set link.status = :expiredStatus
            where link.memoryId = :memoryId
              and link.status = :activeStatus
              and link.expiresAt is not null
              and link.expiresAt <= :now
            """)
    int expireActiveByMemoryId(
            @Param("memoryId") UUID memoryId,
            @Param("now") Instant now,
            @Param("activeStatus") ShareLinkStatus activeStatus,
            @Param("expiredStatus") ShareLinkStatus expiredStatus
    );
}
