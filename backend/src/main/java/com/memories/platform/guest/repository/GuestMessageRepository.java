package com.memories.platform.guest.repository;

import com.memories.platform.guest.entity.GuestMessage;
import com.memories.platform.guest.entity.GuestMessageStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GuestMessageRepository extends JpaRepository<GuestMessage, UUID> {

    long countByMemoryIdAndIpHashAndCreatedAtGreaterThanEqual(
            UUID memoryId,
            String ipHash,
            Instant createdAt
    );

    List<GuestMessage> findAllByMemoryIdAndStatusOrderByCreatedAtAsc(
            UUID memoryId,
            GuestMessageStatus status
    );

    Page<GuestMessage> findAllByMemoryId(UUID memoryId, Pageable pageable);

    Page<GuestMessage> findAllByMemoryIdAndStatus(
            UUID memoryId,
            GuestMessageStatus status,
            Pageable pageable
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select message
            from GuestMessage message
            where message.id = :messageId
              and message.memoryId = :memoryId
            """)
    Optional<GuestMessage> findForUpdateByIdAndMemoryId(
            @Param("messageId") UUID messageId,
            @Param("memoryId") UUID memoryId
    );

    @Modifying
    @Query("""
            update GuestMessage message
            set message.ipHash = null
            where message.ipHash is not null
              and message.createdAt < :threshold
            """)
    int clearExpiredIpHashes(@Param("threshold") Instant threshold);
}
