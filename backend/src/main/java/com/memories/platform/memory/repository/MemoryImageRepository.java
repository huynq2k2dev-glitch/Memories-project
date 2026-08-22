package com.memories.platform.memory.repository;

import com.memories.platform.memory.entity.MemoryImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MemoryImageRepository extends JpaRepository<MemoryImage, UUID> {

    List<MemoryImage> findAllByMemoryIdOrderBySortOrderAsc(UUID memoryId);

    Optional<MemoryImage> findByIdAndMemoryId(UUID id, UUID memoryId);

    boolean existsByMediaAssetId(UUID mediaAssetId);
}
