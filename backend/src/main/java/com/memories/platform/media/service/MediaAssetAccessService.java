package com.memories.platform.media.service;

import com.memories.platform.media.dto.ReadyMediaAsset;
import com.memories.platform.media.dto.ReadyMediaAssetMetadata;
import com.memories.platform.media.entity.MediaAsset;
import com.memories.platform.media.entity.MediaAssetStatus;
import com.memories.platform.media.exception.MediaAssetNotReadyException;
import com.memories.platform.media.repository.MediaAssetRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class MediaAssetAccessService {

    private final MediaAssetRepository assetRepository;
    private final MediaObjectStorageService objectStorageService;

    public MediaAssetAccessService(
            MediaAssetRepository assetRepository,
            MediaObjectStorageService objectStorageService
    ) {
        this.assetRepository = assetRepository;
        this.objectStorageService = objectStorageService;
    }

    @Transactional(propagation = Propagation.MANDATORY, readOnly = true)
    public ReadyMediaAsset requireReadyOwned(UUID assetId, UUID ownerId) {
        MediaAsset asset = assetRepository.findReadyOwnedForShare(assetId, ownerId)
                .orElseThrow(MediaAssetNotReadyException::new);
        return toReadyAsset(asset);
    }

    @Transactional(propagation = Propagation.MANDATORY, readOnly = true)
    public Map<UUID, ReadyMediaAsset> readyOwned(
            UUID ownerId,
            Collection<UUID> assetIds
    ) {
        if (assetIds.isEmpty()) {
            return Map.of();
        }
        List<MediaAsset> assets = assetRepository
                .findAllByIdInAndOwnerIdAndStatusAndDeletedAtIsNull(
                        assetIds,
                        ownerId,
                        MediaAssetStatus.READY
                );
        if (assets.size() != assetIds.stream().distinct().count()) {
            throw new MediaAssetNotReadyException();
        }
        return assets.stream().map(this::toReadyAsset).collect(
                Collectors.toUnmodifiableMap(ReadyMediaAsset::id, Function.identity())
        );
    }

    @Transactional(propagation = Propagation.MANDATORY, readOnly = true)
    public Map<UUID, ReadyMediaAssetMetadata> readyOwnedMetadata(
            UUID ownerId,
            Collection<UUID> assetIds
    ) {
        if (assetIds.isEmpty()) {
            return Map.of();
        }
        List<MediaAsset> assets = assetRepository
                .findAllByIdInAndOwnerIdAndStatusAndDeletedAtIsNull(
                        assetIds,
                        ownerId,
                        MediaAssetStatus.READY
                );
        if (assets.size() != assetIds.stream().distinct().count()) {
            throw new MediaAssetNotReadyException();
        }
        return assets.stream().map(asset -> new ReadyMediaAssetMetadata(
                asset.getId(),
                asset.getMimeType(),
                asset.getFileSize()
        )).collect(Collectors.toUnmodifiableMap(
                ReadyMediaAssetMetadata::id,
                Function.identity()
        ));
    }

    private ReadyMediaAsset toReadyAsset(MediaAsset asset) {
        return new ReadyMediaAsset(
                asset.getId(),
                asset.getMimeType(),
                asset.getFileSize(),
                objectStorageService.presignDelivery(asset),
                asset.getVersion()
        );
    }
}
