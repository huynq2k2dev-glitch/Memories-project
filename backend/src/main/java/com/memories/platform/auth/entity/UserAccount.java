package com.memories.platform.auth.entity;

import com.memories.platform.auth.constants.AuthConstants;

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

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserAccount {

    @Id
    private UUID id;

    @Column(length = 320)
    private String email;

    @Column(name = "password_hash", length = 100)
    private String passwordHash;

    @Column(name = "display_name", nullable = false, length = 120)
    private String displayName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private UserStatus status;

    @Column(nullable = false, length = 20)
    private String locale;

    @Column(nullable = false, length = 64)
    private String timezone;

    @Column(name = "failed_login_count", nullable = false)
    private int failedLoginCount;

    @Column(name = "email_verified_at")
    private Instant emailVerifiedAt;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    @Column(name = "locked_until")
    private Instant lockedUntil;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Version
    @Column(nullable = false)
    private long version;

    public UserAccount(
            UUID id,
            String email,
            String passwordHash,
            String displayName,
            Instant now
    ) {
        this.id = id;
        this.email = email;
        this.passwordHash = passwordHash;
        this.displayName = displayName;
        this.status = UserStatus.PENDING_VERIFICATION;
        this.locale = AuthConstants.DEFAULT_LOCALE;
        this.timezone = AuthConstants.DEFAULT_TIMEZONE;
        this.failedLoginCount = 0;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public boolean isActive() {
        return status == UserStatus.ACTIVE && deletedAt == null;
    }

    public boolean isDeleted() {
        return status == UserStatus.DELETED || deletedAt != null;
    }

    public boolean canVerifyEmail(String target) {
        return status == UserStatus.PENDING_VERIFICATION
                && deletedAt == null
                && email != null
                && email.equals(target);
    }

    public void verifyEmail(Instant verifiedAt) {
        this.emailVerifiedAt = verifiedAt;
        this.status = UserStatus.ACTIVE;
        this.updatedAt = verifiedAt;
    }

    public void clearExpiredTemporaryLock(Instant now) {
        if (lockedUntil != null && !now.isBefore(lockedUntil)) {
            failedLoginCount = 0;
            lockedUntil = null;
            updatedAt = now;
        }
    }

    public boolean isTemporarilyLocked(Instant now) {
        return lockedUntil != null && now.isBefore(lockedUntil);
    }

    public void recordFailedLogin(Instant now, int maximumAttempts, Duration lockDuration) {
        failedLoginCount++;
        if (failedLoginCount >= maximumAttempts) {
            lockedUntil = now.plus(lockDuration);
        }
        updatedAt = now;
    }

    public void recordSuccessfulLogin(Instant now) {
        failedLoginCount = 0;
        lockedUntil = null;
        lastLoginAt = now;
        updatedAt = now;
    }

    public void lock(Instant now) {
        if (!isDeleted()) {
            status = UserStatus.LOCKED;
            updatedAt = now;
        }
    }
}
