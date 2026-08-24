package com.memories.platform.media.repository;

import com.memories.platform.media.entity.MediaAsset;
import com.memories.platform.media.entity.MediaAssetStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MediaAssetRepository extends JpaRepository<MediaAsset, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select asset
            from MediaAsset asset
            where asset.id = :assetId
              and asset.ownerId = :ownerId
              and asset.deletedAt is null
            """)
    Optional<MediaAsset> findOwnedForUpdate(
            @Param("assetId") UUID assetId,
            @Param("ownerId") UUID ownerId
    );

    @Lock(LockModeType.PESSIMISTIC_READ)
    @Query("""
            select asset
            from MediaAsset asset
            where asset.id = :assetId
              and asset.ownerId = :ownerId
              and asset.status = com.memories.platform.media.entity.MediaAssetStatus.READY
              and asset.deletedAt is null
            """)
    Optional<MediaAsset> findReadyOwnedForShare(
            @Param("assetId") UUID assetId,
            @Param("ownerId") UUID ownerId
    );

    List<MediaAsset> findAllByOwnerIdAndStatusAndUploadExpiresAtBefore(
            UUID ownerId,
            MediaAssetStatus status,
            Instant expiresAt
    );

    long countByOwnerIdAndStatusInAndDeletedAtIsNull(
            UUID ownerId,
            Collection<MediaAssetStatus> statuses
    );

    @Query("""
            select coalesce(sum(asset.fileSize), 0)
            from MediaAsset asset
            where asset.ownerId = :ownerId
              and asset.status in :statuses
              and asset.deletedAt is null
            """)
    long sumFileSizeByOwnerAndStatuses(
            @Param("ownerId") UUID ownerId,
            @Param("statuses") Collection<MediaAssetStatus> statuses
    );

    List<MediaAsset> findAllByIdInAndStatusAndDeletedAtIsNull(
            Collection<UUID> ids,
            MediaAssetStatus status
    );

    Optional<MediaAsset> findByIdAndStatusAndDeletedAtIsNull(
            UUID id,
            MediaAssetStatus status
    );
}
