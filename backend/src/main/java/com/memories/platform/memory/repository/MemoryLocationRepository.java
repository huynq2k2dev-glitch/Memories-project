package com.memories.platform.memory.repository;

import com.memories.platform.memory.entity.MemoryLocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MemoryLocationRepository extends JpaRepository<MemoryLocation, UUID> {

    List<MemoryLocation> findAllByMemoryIdOrderBySortOrderAsc(UUID memoryId);

    Optional<MemoryLocation> findByIdAndMemoryId(UUID id, UUID memoryId);

    @Lock(LockModeType.PESSIMISTIC_READ)
    @Query("""
            select location
            from MemoryLocation location
            where location.id = :locationId
              and location.memoryId = :memoryId
            """)
    Optional<MemoryLocation> findReference(
            @Param("locationId") UUID locationId,
            @Param("memoryId") UUID memoryId
    );
}
