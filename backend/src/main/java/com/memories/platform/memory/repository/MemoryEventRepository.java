package com.memories.platform.memory.repository;

import com.memories.platform.memory.entity.MemoryEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MemoryEventRepository extends JpaRepository<MemoryEvent, UUID> {

    List<MemoryEvent> findAllByMemoryIdOrderBySortOrderAsc(UUID memoryId);

    Optional<MemoryEvent> findByIdAndMemoryId(UUID id, UUID memoryId);
}
