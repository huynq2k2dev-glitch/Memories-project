package com.memories.platform.guest.repository;

import com.memories.platform.guest.entity.MemoryGuest;
import com.memories.platform.guest.entity.MemoryGuestStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.List;
import java.util.UUID;

public interface MemoryGuestRepository extends JpaRepository<MemoryGuest, UUID> {

    Page<MemoryGuest> findAllByMemoryId(UUID memoryId, Pageable pageable);

    Optional<MemoryGuest> findByAccessTokenHashAndStatus(
            String accessTokenHash,
            MemoryGuestStatus status
    );

    List<MemoryGuest> findAllByMemoryIdAndStatusOrderByFullNameAsc(
            UUID memoryId,
            MemoryGuestStatus status
    );

    boolean existsByIdAndMemoryIdAndStatus(
            UUID id,
            UUID memoryId,
            MemoryGuestStatus status
    );

    Optional<MemoryGuest> findByIdAndMemoryIdAndStatus(
            UUID id,
            UUID memoryId,
            MemoryGuestStatus status
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select guest
            from MemoryGuest guest
            where guest.id = :guestId
              and guest.memoryId = :memoryId
            """)
    Optional<MemoryGuest> findForUpdateByIdAndMemoryId(
            @Param("guestId") UUID guestId,
            @Param("memoryId") UUID memoryId
    );
}
