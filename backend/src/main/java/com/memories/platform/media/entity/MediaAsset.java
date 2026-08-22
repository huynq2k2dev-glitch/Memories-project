package com.memories.platform.media.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "media_assets")
public class MediaAsset {

    @Id
    private UUID id;

    @Column(name = "owner_id", nullable = false, updatable = false)
    private UUID ownerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "storage_provider", nullable = false, length = 20, updatable = false)
    private MediaStorageProvider storageProvider;

    @Column(name = "bucket_name", nullable = false, length = 100, updatable = false)
    private String bucketName;

    @Column(name = "object_key", nullable = false, length = 1024, updatable = false)
    private String objectKey;

    @Column(name = "original_file_name", nullable = false, length = 255, updatable = false)
    private String originalFileName;

    @Column(name = "mime_type", nullable = false, length = 100, updatable = false)
    private String mimeType;

    @Column(name = "file_size", nullable = false, updatable = false)
    private long fileSize;

    private Integer width;

    private Integer height;

    @Column(length = 128, updatable = false)
    private String checksum;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MediaAssetStatus status;

    @Column(name = "parent_asset_id", updatable = false)
    private UUID parentAssetId;

    @Enumerated(EnumType.STRING)
    @Column(name = "variant_type", nullable = false, length = 20, updatable = false)
    private MediaVariantType variantType;

    @Column(name = "upload_expires_at")
    private Instant uploadExpiresAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "created_by", updatable = false)
    private UUID createdBy;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "updated_by")
    private UUID updatedBy;

    @Version
    @Column(nullable = false)
    private long version;

    protected MediaAsset() {
    }

    public MediaAsset(
            UUID id,
            UUID ownerId,
            MediaStorageProvider storageProvider,
            String bucketName,
            String objectKey,
            String originalFileName,
            String mimeType,
            long fileSize,
            String checksum,
            Instant uploadExpiresAt,
            Instant now
    ) {
        this.id = id;
        this.ownerId = ownerId;
        this.storageProvider = storageProvider;
        this.bucketName = bucketName;
        this.objectKey = objectKey;
        this.originalFileName = originalFileName;
        this.mimeType = mimeType;
        this.fileSize = fileSize;
        this.checksum = checksum;
        this.status = MediaAssetStatus.UPLOADING;
        this.variantType = MediaVariantType.ORIGINAL;
        this.uploadExpiresAt = uploadExpiresAt;
        this.createdAt = now;
        this.createdBy = ownerId;
        this.updatedAt = now;
        this.updatedBy = ownerId;
    }

    public void markReady(UUID actorId, Instant now) {
        status = MediaAssetStatus.READY;
        uploadExpiresAt = null;
        updatedAt = now;
        updatedBy = actorId;
    }

    public void markFailed(UUID actorId, Instant now) {
        status = MediaAssetStatus.FAILED;
        uploadExpiresAt = null;
        updatedAt = now;
        updatedBy = actorId;
    }

    public void softDelete(UUID actorId, Instant now) {
        status = MediaAssetStatus.DELETED;
        uploadExpiresAt = null;
        deletedAt = now;
        updatedAt = now;
        updatedBy = actorId;
    }

    public UUID getId() {
        return id;
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public MediaStorageProvider getStorageProvider() {
        return storageProvider;
    }

    public String getBucketName() {
        return bucketName;
    }

    public String getObjectKey() {
        return objectKey;
    }

    public String getOriginalFileName() {
        return originalFileName;
    }

    public String getMimeType() {
        return mimeType;
    }

    public long getFileSize() {
        return fileSize;
    }

    public String getChecksum() {
        return checksum;
    }

    public MediaAssetStatus getStatus() {
        return status;
    }

    public Instant getUploadExpiresAt() {
        return uploadExpiresAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public long getVersion() {
        return version;
    }
}
