package com.memories.platform.auth.service;

import com.memories.platform.auth.constants.AuthConstants;
import com.memories.platform.auth.entity.AuthAuditEvent;
import com.memories.platform.auth.entity.UserAccount;
import com.memories.platform.auth.repository.AuthAuditEventRepository;
import com.memories.platform.auth.repository.RefreshTokenRepository;
import com.memories.platform.auth.repository.UserAccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class UserAccountLockPersistenceService {

    private final UserAccountRepository userAccountRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final AuthAuditEventRepository auditEventRepository;
    private final Clock clock;

    public UserAccountLockPersistenceService(
            UserAccountRepository userAccountRepository,
            RefreshTokenRepository refreshTokenRepository,
            AuthAuditEventRepository auditEventRepository,
            Clock clock
    ) {
        this.userAccountRepository = userAccountRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.auditEventRepository = auditEventRepository;
        this.clock = clock;
    }

    @Transactional
    public LockResult lock(UUID actorId, UUID targetUserId, String correlationId) {
        Instant now = clock.instant();
        Optional<UserAccount> matchedAccount = userAccountRepository.findForUpdateById(targetUserId);
        if (matchedAccount.isEmpty() || matchedAccount.get().isDeleted()) {
            recordAudit(
                    actorId,
                    targetUserId,
                    AuthConstants.AUDIT_FAILURE,
                    AuthConstants.REASON_ACCOUNT_NOT_FOUND,
                    correlationId,
                    now
            );
            return LockResult.notFound();
        }

        UserAccount targetAccount = matchedAccount.get();
        targetAccount.lock(now);
        refreshTokenRepository.revokeAllActiveForUser(
                targetUserId,
                now,
                AuthConstants.REFRESH_REVOKED_ACCOUNT_LOCKED
        );
        recordAudit(
                actorId,
                targetUserId,
                AuthConstants.AUDIT_SUCCESS,
                null,
                correlationId,
                now
        );
        return LockResult.locked(targetUserId);
    }

    private void recordAudit(
            UUID actorId,
            UUID targetUserId,
            String outcome,
            String reason,
            String correlationId,
            Instant now
    ) {
        auditEventRepository.save(new AuthAuditEvent(
                UUID.randomUUID(),
                AuthConstants.AUDIT_ACCOUNT_LOCK,
                outcome,
                reason,
                targetUserId,
                correlationId,
                now,
                actorId,
                targetUserId
        ));
    }

    public record LockResult(LockOutcome outcome, UUID userId) {
        static LockResult locked(UUID userId) {
            return new LockResult(LockOutcome.LOCKED, userId);
        }

        static LockResult notFound() {
            return new LockResult(LockOutcome.NOT_FOUND, null);
        }
    }

    public enum LockOutcome {
        LOCKED,
        NOT_FOUND
    }
}
