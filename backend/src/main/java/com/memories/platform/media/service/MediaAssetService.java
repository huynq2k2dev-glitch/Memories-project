package com.memories.platform.media.service;

import com.memories.platform.auth.service.CurrentActorService;
import com.memories.platform.media.entity.MediaAsset;
import com.memories.platform.media.exception.MediaAssetInUseException;
import com.memories.platform.media.exception.MediaAssetNotFoundException;
import com.memories.platform.media.exception.MediaVersionConflictException;
import com.memories.platform.media.repository.MediaAssetRepository;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.UUID;

@Service
public class MediaAssetService {

    private final MediaAssetRepository assetRepository;
    private final MediaAssetUsageService usageService;
    private final CurrentActorService currentActorService;
    private final Clock clock;

    public MediaAssetService(
            MediaAssetRepository assetRepository,
            MediaAssetUsageService usageService,
            CurrentActorService currentActorService,
            Clock clock
    ) {
        this.assetRepository = assetRepository;
        this.usageService = usageService;
        this.currentActorService = currentActorService;
        this.clock = clock;
    }

    @Transactional
    public void softDelete(UUID assetId, long version) {
        UUID ownerId = currentActorService.userId();
        MediaAsset asset = assetRepository.findOwnedForUpdate(assetId, ownerId)
                .orElseThrow(MediaAssetNotFoundException::new);
        if (asset.getVersion() != version) {
            throw new MediaVersionConflictException(asset.getVersion());
        }
        if (usageService.isInUse(assetId)) {
            throw new MediaAssetInUseException();
        }
        asset.softDelete(ownerId, clock.instant());
        try {
            assetRepository.flush();
        } catch (ObjectOptimisticLockingFailureException exception) {
            throw new MediaVersionConflictException(null);
        }
    }
}
