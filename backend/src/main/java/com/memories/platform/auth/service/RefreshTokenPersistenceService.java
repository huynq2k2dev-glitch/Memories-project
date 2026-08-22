package com.memories.platform.auth.service;

import com.memories.platform.auth.constants.AuthConstants;
import com.memories.platform.auth.entity.AuthAuditEvent;
import com.memories.platform.auth.entity.RefreshToken;
import com.memories.platform.auth.entity.UserAccount;
import com.memories.platform.auth.repository.AuthAuditEventRepository;
import com.memories.platform.auth.repository.RefreshTokenRepository;
import com.memories.platform.auth.repository.UserAccountRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class RefreshTokenPersistenceService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserAccountRepository userAccountRepository;
    private final AuthAuditEventRepository auditEventRepository;
    private final Clock clock;
    private final Duration familyTtl;

    public RefreshTokenPersistenceService(
            RefreshTokenRepository refreshTokenRepository,
            UserAccountRepository userAccountRepository,
            AuthAuditEventRepository auditEventRepository,
            Clock clock,
            @Value("${platform.auth.refresh-token-family-ttl}") Duration familyTtl
    ) {
        if (familyTtl.isZero() || familyTtl.isNegative()) {
            throw new IllegalArgumentException("Refresh token family TTL must be positive");
        }
        this.refreshTokenRepository = refreshTokenRepository;
        this.userAccountRepository = userAccountRepository;
        this.auditEventRepository = auditEventRepository;
        this.clock = clock;
        this.familyTtl = familyTtl;
    }

    @Transactional
    public StoredRefreshToken issueInitial(UUID userId, String tokenHash) {
        Instant now = clock.instant();
        UserAccount user = userAccountRepository.findForUpdateById(userId)
                .filter(UserAccount::isActive)
                .orElseThrow(() -> new IllegalStateException("Active login user no longer exists"));
        Instant expiresAt = now.plus(familyTtl);
        refreshTokenRepository.save(new RefreshToken(
                UUID.randomUUID(),
                user,
                UUID.randomUUID(),
                null,
                tokenHash,
                expiresAt,
                now
        ));
        return new StoredRefreshToken(userId, expiresAt);
    }

    @Transactional
    public RotationResult rotate(
            String currentTokenHash,
            String replacementTokenHash,
            String correlationId
    ) {
        Instant now = clock.instant();
        Optional<UUID> candidateUserId = refreshTokenRepository.findUserIdByTokenHash(currentTokenHash);
        if (candidateUserId.isEmpty()) {
            recordAudit(null, AuthConstants.AUDIT_FAILURE, AuthConstants.REASON_SESSION_INVALID, correlationId, now);
            return RotationResult.invalid();
        }

        UserAccount user = userAccountRepository.findForUpdateById(candidateUserId.get()).orElse(null);
        RefreshToken currentToken = refreshTokenRepository.findForUpdateByTokenHash(currentTokenHash).orElse(null);
        if (user == null || currentToken == null) {
            recordAudit(null, AuthConstants.AUDIT_FAILURE, AuthConstants.REASON_SESSION_INVALID, correlationId, now);
            return RotationResult.invalid();
        }

        if (currentToken.isRevoked()) {
            if (currentToken.wasRotated()) {
                refreshTokenRepository.revokeActiveFamily(
                        currentToken.getFamilyId(),
                        now,
                        AuthConstants.REFRESH_REVOKED_REUSE
                );
                recordAudit(
                        user.getId(),
                        AuthConstants.AUDIT_FAILURE,
                        AuthConstants.REASON_REFRESH_TOKEN_REUSE,
                        correlationId,
                        now
                );
                return RotationResult.reuseDetected();
            }
            recordAudit(user.getId(), AuthConstants.AUDIT_FAILURE, AuthConstants.REASON_SESSION_INVALID, correlationId, now);
            return RotationResult.invalid();
        }

        if (currentToken.isExpiredAt(now)) {
            currentToken.revoke(now, AuthConstants.REFRESH_REVOKED_EXPIRED);
            recordAudit(user.getId(), AuthConstants.AUDIT_FAILURE, AuthConstants.REASON_SESSION_INVALID, correlationId, now);
            return RotationResult.invalid();
        }

        if (!user.isActive()) {
            refreshTokenRepository.revokeActiveFamily(
                    currentToken.getFamilyId(),
                    now,
                    AuthConstants.REFRESH_REVOKED_ACCOUNT_INACTIVE
            );
            recordAudit(user.getId(), AuthConstants.AUDIT_FAILURE, AuthConstants.REASON_SESSION_INVALID, correlationId, now);
            return RotationResult.invalid();
        }

        currentToken.revoke(now, AuthConstants.REFRESH_REVOKED_ROTATED);
        refreshTokenRepository.save(new RefreshToken(
                UUID.randomUUID(),
                user,
                currentToken.getFamilyId(),
                currentToken,
                replacementTokenHash,
                currentToken.getExpiresAt(),
                now
        ));
        recordAudit(user.getId(), AuthConstants.AUDIT_SUCCESS, null, correlationId, now);
        return RotationResult.success(user.getId(), currentToken.getExpiresAt());
    }

    @Transactional
    public void revokeCurrent(String tokenHash, String correlationId) {
        Instant now = clock.instant();
        Optional<UUID> candidateUserId = refreshTokenRepository.findUserIdByTokenHash(tokenHash);
        if (candidateUserId.isEmpty()) {
            return;
        }
        userAccountRepository.findForUpdateById(candidateUserId.get());
        refreshTokenRepository.findForUpdateByTokenHash(tokenHash).ifPresent(token -> {
            token.revoke(now, AuthConstants.REFRESH_REVOKED_LOGOUT);
            recordLogoutAudit(token.getUser().getId(), correlationId, now);
        });
    }

    @Transactional
    public void revokeAll(UUID userId, String correlationId) {
        Instant now = clock.instant();
        userAccountRepository.findForUpdateById(userId)
                .orElseThrow(() -> new IllegalStateException("Authenticated user no longer exists"));
        refreshTokenRepository.revokeAllActiveForUser(
                userId,
                now,
                AuthConstants.REFRESH_REVOKED_LOGOUT_ALL
        );
        recordLogoutAudit(userId, correlationId, now);
    }

    private void recordLogoutAudit(UUID userId, String correlationId, Instant now) {
        auditEventRepository.save(new AuthAuditEvent(
                UUID.randomUUID(),
                AuthConstants.AUDIT_LOGOUT,
                AuthConstants.AUDIT_SUCCESS,
                null,
                userId,
                correlationId,
                now
        ));
    }

    private void recordAudit(
            UUID userId,
            String outcome,
            String reason,
            String correlationId,
            Instant now
    ) {
        auditEventRepository.save(new AuthAuditEvent(
                UUID.randomUUID(),
                AuthConstants.AUDIT_REFRESH,
                outcome,
                reason,
                userId,
                correlationId,
                now
        ));
    }

    public record StoredRefreshToken(UUID userId, Instant expiresAt) {
    }

    public record RotationResult(
            RotationOutcome outcome,
            UUID userId,
            Instant expiresAt
    ) {
        static RotationResult success(UUID userId, Instant expiresAt) {
            return new RotationResult(RotationOutcome.SUCCESS, userId, expiresAt);
        }

        static RotationResult invalid() {
            return new RotationResult(RotationOutcome.INVALID, null, null);
        }

        static RotationResult reuseDetected() {
            return new RotationResult(RotationOutcome.REUSE_DETECTED, null, null);
        }
    }

    public enum RotationOutcome {
        SUCCESS,
        INVALID,
        REUSE_DETECTED
    }
}
