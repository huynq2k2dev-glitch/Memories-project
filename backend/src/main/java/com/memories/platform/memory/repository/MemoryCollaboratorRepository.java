package com.memories.platform.memory.repository;

import com.memories.platform.memory.entity.MemoryCollaborator;
import com.memories.platform.memory.entity.MemoryCollaboratorStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MemoryCollaboratorRepository extends JpaRepository<MemoryCollaborator, UUID> {

    Optional<MemoryCollaborator> findByMemoryIdAndUserIdAndStatus(
            UUID memoryId,
            UUID userId,
            MemoryCollaboratorStatus status
    );

    List<MemoryCollaborator> findAllByMemoryIdOrderByCreatedAtAsc(UUID memoryId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select collaborator
            from MemoryCollaborator collaborator
            where collaborator.memoryId = :memoryId
              and collaborator.userId = :userId
            """)
    Optional<MemoryCollaborator> findForUpdateByMemoryIdAndUserId(
            @Param("memoryId") UUID memoryId,
            @Param("userId") UUID userId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select collaborator
            from MemoryCollaborator collaborator
            where collaborator.id = :collaboratorId
              and collaborator.memoryId = :memoryId
            """)
    Optional<MemoryCollaborator> findForUpdateByIdAndMemoryId(
            @Param("collaboratorId") UUID collaboratorId,
            @Param("memoryId") UUID memoryId
    );
}
