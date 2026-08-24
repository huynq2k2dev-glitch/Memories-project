package com.memories.platform.memory.service;

import com.memories.platform.media.dto.ReadyMediaAsset;
import com.memories.platform.media.service.MediaAssetAccessService;
import com.memories.platform.memory.dto.CreateMemoryImageRequest;
import com.memories.platform.memory.dto.MemoryImageResponse;
import com.memories.platform.memory.dto.ReorderMemoryItemsRequest;
import com.memories.platform.memory.dto.UpdateMemoryImageRequest;
import com.memories.platform.memory.entity.MemoryImage;
import com.memories.platform.memory.exception.InvalidMemoryItemOrderException;
import com.memories.platform.memory.exception.InvalidMemorySectionReferenceException;
import com.memories.platform.memory.exception.MemoryImageConflictException;
import com.memories.platform.memory.exception.MemoryImageNotFoundException;
import com.memories.platform.memory.exception.MemoryVersionConflictException;
import com.memories.platform.memory.repository.MemoryImageRepository;
import com.memories.platform.memory.repository.MemorySectionRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class MemoryImageService {

    private final MemoryImageRepository imageRepository;
    private final MemorySectionRepository sectionRepository;
    private final MemoryAccessService accessService;
    private final MemoryContentSafetyService contentSafetyService;
    private final MediaAssetAccessService assetAccessService;
    private final Clock clock;

    public MemoryImageService(
            MemoryImageRepository imageRepository,
            MemorySectionRepository sectionRepository,
            MemoryAccessService accessService,
            MemoryContentSafetyService contentSafetyService,
            MediaAssetAccessService assetAccessService,
            Clock clock
    ) {
        this.imageRepository = imageRepository;
        this.sectionRepository = sectionRepository;
        this.accessService = accessService;
        this.contentSafetyService = contentSafetyService;
        this.assetAccessService = assetAccessService;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<MemoryImageResponse> list(UUID memoryId) {
        accessService.requireView(memoryId);
        List<MemoryImage> images = imageRepository.findAllByMemoryIdOrderBySortOrderAsc(memoryId);
        Map<UUID, ReadyMediaAsset> assets = assetAccessService.ready(
                images.stream().map(MemoryImage::getMediaAssetId).toList()
        );
        return images.stream().map(image -> toResponse(
                image,
                assets.get(image.getMediaAssetId())
        )).toList();
    }

    @Transactional
    public MemoryImageResponse create(UUID memoryId, CreateMemoryImageRequest request) {
        accessService.requireEditable(memoryId);
        validateSection(memoryId, request.sectionId());
        requireSafe(request.caption(), request.altText());
        ReadyMediaAsset asset = assetAccessService.requireReadyOwned(
                request.assetId(),
                accessService.actorId()
        );
        UUID actorId = accessService.actorId();
        Instant now = clock.instant();
        MemoryImage image = new MemoryImage(
                UUID.randomUUID(),
                memoryId,
                request.assetId(),
                request.sectionId(),
                request.caption(),
                request.altText(),
                request.sortOrder(),
                request.coverCandidate(),
                actorId,
                now
        );
        try {
            imageRepository.saveAndFlush(image);
        } catch (DataIntegrityViolationException exception) {
            throw new MemoryImageConflictException();
        }
        return toResponse(image, asset);
    }

    @Transactional
    public MemoryImageResponse update(
            UUID memoryId,
            UUID imageId,
            UpdateMemoryImageRequest request
    ) {
        accessService.requireEditable(memoryId);
        MemoryImage image = find(memoryId, imageId);
        requireVersion(image, request.version());
        validateSection(memoryId, request.sectionId());
        requireSafe(request.caption(), request.altText());
        image.update(
                request.sectionId(),
                request.caption(),
                request.altText(),
                request.coverCandidate(),
                accessService.actorId(),
                clock.instant()
        );
        flushForUpdate();
        ReadyMediaAsset asset = assetAccessService.requireReady(image.getMediaAssetId());
        return toResponse(image, asset);
    }

    @Transactional
    public void delete(UUID memoryId, UUID imageId, long version) {
        accessService.requireEditable(memoryId);
        MemoryImage image = find(memoryId, imageId);
        requireVersion(image, version);
        imageRepository.delete(image);
        flushForUpdate();
    }

    @Transactional
    public List<MemoryImageResponse> reorder(
            UUID memoryId,
            ReorderMemoryItemsRequest request
    ) {
        accessService.requireEditable(memoryId);
        List<MemoryImage> images = imageRepository.findAllByMemoryIdOrderBySortOrderAsc(memoryId);
        validateOrder(images, request);
        List<UUID> currentOrder = images.stream().map(MemoryImage::getId).toList();
        if (!currentOrder.equals(request.orderedIds())) {
            int temporaryBase = temporaryBase(
                    images.stream().mapToInt(MemoryImage::getSortOrder).max().orElse(0),
                    images.size()
            );
            UUID actorId = accessService.actorId();
            Instant now = clock.instant();
            Map<UUID, MemoryImage> imagesById = images.stream().collect(
                    java.util.stream.Collectors.toMap(MemoryImage::getId, image -> image)
            );
            for (int index = 0; index < request.orderedIds().size(); index++) {
                imagesById.get(request.orderedIds().get(index))
                        .reorder(temporaryBase + index, actorId, now);
            }
            flushForUpdate();
            for (int index = 0; index < request.orderedIds().size(); index++) {
                imagesById.get(request.orderedIds().get(index)).reorder(index, actorId, now);
            }
            flushForUpdate();
            images.sort(Comparator.comparingInt(MemoryImage::getSortOrder));
        }
        Map<UUID, ReadyMediaAsset> assets = assetAccessService.ready(
                images.stream().map(MemoryImage::getMediaAssetId).toList()
        );
        return images.stream().map(image -> toResponse(
                image,
                assets.get(image.getMediaAssetId())
        )).toList();
    }

    private void validateOrder(List<MemoryImage> images, ReorderMemoryItemsRequest request) {
        Set<UUID> entityIds = images.stream()
                .map(MemoryImage::getId)
                .collect(java.util.stream.Collectors.toSet());
        Set<UUID> orderedIds = new HashSet<>(request.orderedIds());
        if (orderedIds.size() != request.orderedIds().size()
                || !orderedIds.equals(entityIds)
                || !request.versions().keySet().equals(entityIds)) {
            throw new InvalidMemoryItemOrderException();
        }
        images.forEach(image -> requireVersion(image, request.versions().get(image.getId())));
    }

    private void validateSection(UUID memoryId, UUID sectionId) {
        if (sectionId != null && sectionRepository.findByIdAndMemoryId(sectionId, memoryId).isEmpty()) {
            throw new InvalidMemorySectionReferenceException();
        }
    }

    private MemoryImage find(UUID memoryId, UUID imageId) {
        return imageRepository.findByIdAndMemoryId(imageId, memoryId)
                .orElseThrow(MemoryImageNotFoundException::new);
    }

    private void requireVersion(MemoryImage image, Long expectedVersion) {
        if (expectedVersion == null || image.getVersion() != expectedVersion) {
            throw new MemoryVersionConflictException(image.getVersion());
        }
    }

    private void requireSafe(String... values) {
        for (String value : values) {
            contentSafetyService.requireSafeMarkdown(value);
        }
    }

    private int temporaryBase(int maximumSortOrder, int itemCount) {
        long base = (long) maximumSortOrder + itemCount + 1;
        if (base + itemCount > Integer.MAX_VALUE) {
            throw new InvalidMemoryItemOrderException();
        }
        return (int) base;
    }

    private void flushForUpdate() {
        try {
            imageRepository.flush();
        } catch (ObjectOptimisticLockingFailureException exception) {
            throw new MemoryVersionConflictException(null);
        } catch (DataIntegrityViolationException exception) {
            throw new MemoryImageConflictException();
        }
    }

    private MemoryImageResponse toResponse(MemoryImage image, ReadyMediaAsset asset) {
        return new MemoryImageResponse(
                image.getId(),
                image.getMediaAssetId(),
                image.getSectionId(),
                image.getCaption(),
                image.getAltText(),
                image.getSortOrder(),
                image.isCoverCandidate(),
                asset.deliveryUrl(),
                asset.version(),
                image.getCreatedAt(),
                image.getUpdatedAt(),
                image.getVersion()
        );
    }
}
