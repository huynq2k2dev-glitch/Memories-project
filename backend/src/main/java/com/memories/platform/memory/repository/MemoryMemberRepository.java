package com.memories.platform.memory.repository;

import com.memories.platform.memory.entity.MemoryMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MemoryMemberRepository extends JpaRepository<MemoryMember, UUID> {

    List<MemoryMember> findAllByMemoryIdOrderBySortOrderAsc(UUID memoryId);

    Optional<MemoryMember> findByIdAndMemoryId(UUID id, UUID memoryId);

    boolean existsByAvatarAssetId(UUID avatarAssetId);
}
