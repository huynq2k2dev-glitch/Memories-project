package com.memories.platform.guest.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "memory_guests")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MemoryGuest {

    @Id
    private UUID id;

    @Column(name = "memory_id", nullable = false, updatable = false)
    private UUID memoryId;

    @Column(name = "full_name", nullable = false, length = 200)
    private String fullName;

    @Column(length = 255)
    private String email;

    @Column(length = 20)
    private String phone;

    @Column(name = "guest_group", length = 100)
    private String guestGroup;

    @Column(name = "max_party_size", nullable = false)
    private int maxPartySize;

    @Column(name = "access_token_hash", unique = true, length = 128)
    private String accessTokenHash;

    @Column(length = 1000)
    private String note;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MemoryGuestStatus status;

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

    public MemoryGuest(
            UUID id,
            UUID memoryId,
            String fullName,
            String email,
            String phone,
            String guestGroup,
            int maxPartySize,
            String note,
            UUID actorId,
            Instant now
    ) {
        this.id = id;
        this.memoryId = memoryId;
        this.fullName = fullName;
        this.email = email;
        this.phone = phone;
        this.guestGroup = guestGroup;
        this.maxPartySize = maxPartySize;
        this.note = note;
        this.status = MemoryGuestStatus.ACTIVE;
        this.createdAt = now;
        this.createdBy = actorId;
        this.updatedAt = now;
        this.updatedBy = actorId;
    }

    public void update(
            String fullName,
            String email,
            String phone,
            String guestGroup,
            int maxPartySize,
            String note,
            UUID actorId,
            Instant now
    ) {
        this.fullName = fullName;
        this.email = email;
        this.phone = phone;
        this.guestGroup = guestGroup;
        this.maxPartySize = maxPartySize;
        this.note = note;
        this.updatedAt = now;
        this.updatedBy = actorId;
    }

    public void issueAccessToken(String tokenHash, UUID actorId, Instant now) {
        this.accessTokenHash = tokenHash;
        this.updatedAt = now;
        this.updatedBy = actorId;
    }

    public void disable(UUID actorId, Instant now) {
        this.status = MemoryGuestStatus.DISABLED;
        this.accessTokenHash = null;
        this.updatedAt = now;
        this.updatedBy = actorId;
    }

    public boolean isActive() {
        return status == MemoryGuestStatus.ACTIVE;
    }

    public boolean hasAccessToken() {
        return accessTokenHash != null;
    }
}
