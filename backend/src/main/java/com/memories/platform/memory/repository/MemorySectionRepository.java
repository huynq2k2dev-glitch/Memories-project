package com.memories.platform.memory.repository;

import com.memories.platform.memory.entity.MemorySection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MemorySectionRepository extends JpaRepository<MemorySection, UUID> {

    List<MemorySection> findAllByMemoryIdOrderBySortOrderAsc(UUID memoryId);

    Optional<MemorySection> findByIdAndMemoryId(UUID id, UUID memoryId);
}
