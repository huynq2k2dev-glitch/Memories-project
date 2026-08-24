package com.memories.platform.media.service;

import com.memories.platform.auth.service.CurrentActorAccountLockService;
import com.memories.platform.auth.service.CurrentActorService;
import com.memories.platform.config.MediaStorageProperties;
import com.memories.platform.config.RateLimitProperties;
import com.memories.platform.media.constants.MediaConstants;
import com.memories.platform.media.dto.InitiateMediaUploadRequest;
import com.memories.platform.media.dto.InitiateMediaUploadResponse;
import com.memories.platform.media.dto.MediaAssetResponse;
import com.memories.platform.media.dto.MediaUploadTarget;
import com.memories.platform.media.entity.MediaAsset;
import com.memories.platform.media.entity.MediaAssetStatus;
import com.memories.platform.media.exception.InvalidMediaUploadException;
import com.memories.platform.media.exception.MediaAssetNotFoundException;
import com.memories.platform.media.exception.MediaAssetNotReadyException;
import com.memories.platform.media.exception.MediaQuotaExceededException;
import com.memories.platform.media.exception.MediaUploadVerificationException;
import com.memories.platform.media.exception.MediaUploadRateLimitException;
import com.memories.platform.media.repository.MediaAssetRepository;
import com.memories.platform.ratelimit.constants.RateLimitScope;
import com.memories.platform.ratelimit.service.RateLimitService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.EnumSet;
import java.util.Locale;
import java.util.UUID;

@Service
public class MediaUploadService {

    private static final EnumSet<MediaAssetStatus> QUOTA_STATUSES = EnumSet.of(
            MediaAssetStatus.UPLOADING,
            MediaAssetStatus.READY
    );

    private final MediaAssetRepository assetRepository;
    private final MediaObjectStorageService objectStorageService;
    private final CurrentActorService currentActorService;
    private final CurrentActorAccountLockService accountLockService;
    private final MediaStorageProperties properties;
    private final RateLimitProperties rateLimitProperties;
    private final RateLimitService rateLimitService;
    private final Clock clock;

    public MediaUploadService(
            MediaAssetRepository assetRepository,
            MediaObjectStorageService objectStorageService,
            CurrentActorService currentActorService,
            CurrentActorAccountLockService accountLockService,
            MediaStorageProperties properties,
            RateLimitProperties rateLimitProperties,
            RateLimitService rateLimitService,
            Clock clock
    ) {
        this.assetRepository = assetRepository;
        this.objectStorageService = objectStorageService;
        this.currentActorService = currentActorService;
        this.accountLockService = accountLockService;
        this.properties = properties;
        this.rateLimitProperties = rateLimitProperties;
        this.rateLimitService = rateLimitService;
        this.clock = clock;
    }

    @Transactional
    public InitiateMediaUploadResponse initiate(InitiateMediaUploadRequest request) {
        UUID ownerId = currentActorService.userId();
        if (!rateLimitService.tryAcquire(
                RateLimitScope.MEDIA_UPLOAD,
                ownerId.toString(),
                rateLimitProperties.uploadCount(),
                rateLimitProperties.uploadWindow()
        )) {
            throw new MediaUploadRateLimitException();
        }
        accountLockService.lockCurrentAccount();
        String mimeType = normalizeMimeType(request.mimeType());
        validate(mimeType, request.fileSize(), request.checksumSha256());
        Instant now = clock.instant();
        expireStaleUploads(ownerId, now);
        requireQuota(ownerId, request.fileSize());

        UUID assetId = UUID.randomUUID();
        Instant expiresAt = now.plus(properties.presignedPutTtl());
        MediaAsset asset = new MediaAsset(
                assetId,
                ownerId,
                properties.provider(),
                properties.bucket(),
                objectKey(ownerId, assetId, mimeType),
                safeFileName(request.originalFileName()),
                mimeType,
                request.fileSize(),
                normalizeChecksum(request.checksumSha256()),
                expiresAt,
                now
        );
        assetRepository.saveAndFlush(asset);
        MediaUploadTarget target = objectStorageService.presignUpload(asset);
        return new InitiateMediaUploadResponse(
                assetId,
                target.uploadUrl(),
                "PUT",
                target.requiredHeaders(),
                expiresAt
        );
    }

    @Transactional(noRollbackFor = MediaUploadVerificationException.class)
    public MediaAssetResponse complete(UUID assetId) {
        UUID ownerId = currentActorService.userId();
        MediaAsset asset = assetRepository.findOwnedForUpdate(assetId, ownerId)
                .orElseThrow(MediaAssetNotFoundException::new);
        if (asset.getStatus() != MediaAssetStatus.UPLOADING) {
            throw new MediaAssetNotReadyException();
        }
        Instant now = clock.instant();
        if (!asset.getUploadExpiresAt().isAfter(now)) {
            asset.markFailed(ownerId, now);
            assetRepository.flush();
            throw new MediaUploadVerificationException();
        }
        try {
            objectStorageService.verifyUpload(asset);
        } catch (MediaUploadVerificationException exception) {
            asset.markFailed(ownerId, now);
            assetRepository.flush();
            throw exception;
        }
        asset.markReady(ownerId, now);
        assetRepository.flush();
        return toResponse(asset);
    }

    private void validate(String mimeType, long fileSize, String checksum) {
        if (!MediaConstants.ALLOWED_IMAGE_MIME_TYPES.contains(mimeType)
                || fileSize < 1
                || fileSize > properties.maxFileSize()) {
            throw new InvalidMediaUploadException();
        }
        normalizeChecksum(checksum);
    }

    private void expireStaleUploads(UUID ownerId, Instant now) {
        assetRepository.findAllByOwnerIdAndStatusAndUploadExpiresAtBefore(
                ownerId,
                MediaAssetStatus.UPLOADING,
                now
        ).forEach(asset -> asset.markFailed(ownerId, now));
        assetRepository.flush();
    }

    private void requireQuota(UUID ownerId, long incomingBytes) {
        long assetCount = assetRepository.countByOwnerIdAndStatusInAndDeletedAtIsNull(
                ownerId,
                QUOTA_STATUSES
        );
        long usedBytes = assetRepository.sumFileSizeByOwnerAndStatuses(
                ownerId,
                QUOTA_STATUSES
        );
        if (assetCount >= properties.maxAssetsPerOwner()
                || incomingBytes > properties.maxBytesPerOwner() - usedBytes) {
            throw new MediaQuotaExceededException();
        }
    }

    private String safeFileName(String originalFileName) {
        String normalized = originalFileName.trim().replace('\\', '/');
        String fileName = normalized.substring(normalized.lastIndexOf('/') + 1).trim();
        if (fileName.isBlank() || fileName.length() > 255) {
            throw new InvalidMediaUploadException();
        }
        return fileName;
    }

    private String objectKey(UUID ownerId, UUID assetId, String mimeType) {
        return "users/" + ownerId + "/" + assetId + MediaConstants.FILE_EXTENSIONS.get(mimeType);
    }

    private String normalizeMimeType(String mimeType) {
        return mimeType.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeChecksum(String checksum) {
        if (checksum == null || checksum.isBlank()) {
            return null;
        }
        try {
            byte[] decoded = Base64.getDecoder().decode(checksum);
            if (decoded.length != 32) {
                throw new InvalidMediaUploadException();
            }
            return Base64.getEncoder().encodeToString(decoded);
        } catch (IllegalArgumentException exception) {
            throw new InvalidMediaUploadException();
        }
    }

    private MediaAssetResponse toResponse(MediaAsset asset) {
        return new MediaAssetResponse(
                asset.getId(),
                asset.getOriginalFileName(),
                asset.getMimeType(),
                asset.getFileSize(),
                asset.getStatus(),
                asset.getCreatedAt(),
                asset.getUpdatedAt(),
                asset.getVersion()
        );
    }
}
