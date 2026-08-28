package com.memories.platform.guest.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "guest_messages")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GuestMessage {

    @Id
    private UUID id;

    @Column(name = "memory_id", nullable = false, updatable = false)
    private UUID memoryId;

    @Column(name = "guest_id", updatable = false)
    private UUID guestId;

    @Column(name = "guest_name", nullable = false, length = 200, updatable = false)
    private String guestName;

    @Column(nullable = false, length = 2000, updatable = false)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private GuestMessageStatus status;

    @Column(name = "ip_hash", length = 128)
    private String ipHash;

    @Column(name = "moderated_by")
    private UUID moderatedBy;

    @Column(name = "moderated_at")
    private Instant moderatedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public GuestMessage(
            UUID id,
            UUID memoryId,
            String guestName,
            String content,
            GuestMessageStatus status,
            String ipHash,
            Instant now
    ) {
        this.id = id;
        this.memoryId = memoryId;
        this.guestName = guestName;
        this.content = content;
        this.status = status;
        this.ipHash = ipHash;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public boolean canTransitionTo(GuestMessageStatus targetStatus) {
        return switch (status) {
            case PENDING -> targetStatus == GuestMessageStatus.APPROVED
                    || targetStatus == GuestMessageStatus.REJECTED;
            case APPROVED -> targetStatus == GuestMessageStatus.HIDDEN;
            case HIDDEN -> targetStatus == GuestMessageStatus.APPROVED;
            case REJECTED -> targetStatus == GuestMessageStatus.PENDING;
        };
    }

    public void moderate(GuestMessageStatus targetStatus, UUID actorId, Instant now) {
        this.status = targetStatus;
        this.moderatedBy = actorId;
        this.moderatedAt = now;
        this.updatedAt = now;
    }
}
