package com.memories.platform.memory.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "memory_locations")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MemoryLocation {

    @Id
    private UUID id;

    @Column(name = "memory_id", nullable = false, updatable = false)
    private UUID memoryId;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(length = 500)
    private String address;

    @Column(precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(precision = 10, scale = 7)
    private BigDecimal longitude;

    @Column(name = "map_url", length = 2048)
    private String mapUrl;

    @Column(length = 1000)
    private String note;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

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

    public MemoryLocation(
            UUID id,
            UUID memoryId,
            String name,
            String address,
            BigDecimal latitude,
            BigDecimal longitude,
            String mapUrl,
            String note,
            int sortOrder,
            UUID actorId,
            Instant now
    ) {
        this.id = id;
        this.memoryId = memoryId;
        this.name = name;
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
        this.mapUrl = mapUrl;
        this.note = note;
        this.sortOrder = sortOrder;
        this.createdAt = now;
        this.createdBy = actorId;
        this.updatedAt = now;
        this.updatedBy = actorId;
    }

    public void update(
            String name,
            String address,
            BigDecimal latitude,
            BigDecimal longitude,
            String mapUrl,
            String note,
            UUID actorId,
            Instant now
    ) {
        this.name = name;
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
        this.mapUrl = mapUrl;
        this.note = note;
        this.updatedAt = now;
        this.updatedBy = actorId;
    }

    public void reorder(int sortOrder, UUID actorId, Instant now) {
        this.sortOrder = sortOrder;
        this.updatedAt = now;
        this.updatedBy = actorId;
    }
}
