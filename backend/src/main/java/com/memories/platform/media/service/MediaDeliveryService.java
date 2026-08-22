package com.memories.platform.media.service;

import com.memories.platform.media.dto.MediaDeliveryResponse;
import com.memories.platform.media.entity.MediaAsset;
import com.memories.platform.media.entity.MediaAssetStatus;
import com.memories.platform.media.exception.MediaAssetNotFoundException;
import com.memories.platform.media.repository.MediaAssetRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class MediaDeliveryService {

    private final MediaAssetRepository assetRepository;
    private final MediaObjectStorageService objectStorageService;

    public MediaDeliveryService(
            MediaAssetRepository assetRepository,
            MediaObjectStorageService objectStorageService
    ) {
        this.assetRepository = assetRepository;
        this.objectStorageService = objectStorageService;
    }

    @Transactional(propagation = Propagation.MANDATORY, readOnly = true)
    public MediaDeliveryResponse delivery(UUID assetId) {
        MediaAsset asset = assetRepository.findByIdAndStatusAndDeletedAtIsNull(
                assetId,
                MediaAssetStatus.READY
        ).orElseThrow(MediaAssetNotFoundException::new);
        return new MediaDeliveryResponse(
                objectStorageService.presignDelivery(asset),
                asset.getMimeType()
        );
    }
}
