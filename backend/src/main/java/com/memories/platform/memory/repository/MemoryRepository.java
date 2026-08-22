package com.memories.platform.memory.repository;

import com.memories.platform.memory.entity.Memory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public interface MemoryRepository extends JpaRepository<Memory, UUID> {

    Optional<Memory> findByIdAndOwnerIdAndDeletedAtIsNull(UUID id, UUID ownerId);

    boolean existsByCoverAssetId(UUID coverAssetId);

    @Query("""
            select memory
            from Memory memory
            where lower(memory.slug) = lower(:slug)
              and memory.status = :status
              and memory.visibility in :visibilities
              and memory.deletedAt is null
              and (memory.expiresAt is null or memory.expiresAt > :now)
            """)
    Optional<Memory> findPublicBySlug(
            @Param("slug") String slug,
            @Param("status") com.memories.platform.memory.entity.MemoryStatus status,
            @Param("visibilities") Collection<com.memories.platform.memory.entity.MemoryVisibility> visibilities,
            @Param("now") Instant now
    );

    @Query("""
            select count(memory) > 0
            from Memory memory
            where memory.status = :status
              and memory.visibility in :visibilities
              and memory.deletedAt is null
              and (memory.expiresAt is null or memory.expiresAt > :now)
              and (
                    memory.coverAssetId = :assetId
                    or exists (
                        select member.id
                        from MemoryMember member
                        where member.memoryId = memory.id
                          and member.avatarAssetId = :assetId
                    )
                    or exists (
                        select image.id
                        from MemoryImage image
                        where image.memoryId = memory.id
                          and image.mediaAssetId = :assetId
                          and (
                                image.sectionId is null
                                or exists (
                                    select section.id
                                    from MemorySection section
                                    where section.id = image.sectionId
                                      and section.memoryId = memory.id
                                      and section.visible = true
                                )
                          )
                    )
              )
            """)
    boolean isAssetPubliclyReferenced(
            @Param("assetId") UUID assetId,
            @Param("status") com.memories.platform.memory.entity.MemoryStatus status,
            @Param("visibilities") Collection<com.memories.platform.memory.entity.MemoryVisibility> visibilities,
            @Param("now") Instant now
    );
}
