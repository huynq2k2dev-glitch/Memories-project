package com.memories.platform.auth.service;

import com.memories.platform.auth.constants.AuthConstants;
import com.memories.platform.auth.entity.AuthAuditEvent;
import com.memories.platform.auth.entity.UserAccount;
import com.memories.platform.auth.entity.UserStatus;
import com.memories.platform.auth.repository.AuthAuditEventRepository;
import com.memories.platform.auth.repository.UserAccountRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class LoginPersistenceService {

    private final UserAccountRepository userAccountRepository;
    private final AuthAuditEventRepository auditEventRepository;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;
    private final int maximumFailedAttempts;
    private final Duration temporaryLockDuration;
    private final String dummyPasswordHash;

    public LoginPersistenceService(
            UserAccountRepository userAccountRepository,
            AuthAuditEventRepository auditEventRepository,
            PasswordEncoder passwordEncoder,
            Clock clock,
            @Value("${platform.auth.max-failed-login-attempts}") int maximumFailedAttempts,
            @Value("${platform.auth.temporary-lock-duration}") Duration temporaryLockDuration
    ) {
        if (maximumFailedAttempts < 1) {
            throw new IllegalArgumentException("Maximum failed login attempts must be positive");
        }
        if (temporaryLockDuration.isZero() || temporaryLockDuration.isNegative()) {
            throw new IllegalArgumentException("Temporary lock duration must be positive");
        }
        this.userAccountRepository = userAccountRepository;
        this.auditEventRepository = auditEventRepository;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
        this.maximumFailedAttempts = maximumFailedAttempts;
        this.temporaryLockDuration = temporaryLockDuration;
        this.dummyPasswordHash = passwordEncoder.encode(UUID.randomUUID().toString());
    }

    @Transactional
    public LoginAttempt authenticate(String normalizedEmail, String password, String correlationId) {
        Instant now = clock.instant();
        Optional<UserAccount> matchedAccount = userAccountRepository.findForUpdateByEmail(normalizedEmail);
        if (matchedAccount.isEmpty()) {
            passwordEncoder.matches(password, dummyPasswordHash);
            recordAudit(null, AuthConstants.AUDIT_FAILURE, AuthConstants.REASON_INVALID_CREDENTIALS, correlationId, now);
            return new LoginAttempt(LoginOutcome.INVALID_CREDENTIALS, null);
        }

        UserAccount user = matchedAccount.get();
        user.clearExpiredTemporaryLock(now);
        boolean passwordMatches = user.getPasswordHash() != null
                && passwordEncoder.matches(password, user.getPasswordHash());

        if (!passwordMatches) {
            if (!user.isTemporarilyLocked(now)) {
                user.recordFailedLogin(now, maximumFailedAttempts, temporaryLockDuration);
            }
            recordAudit(
                    user.getId(),
                    AuthConstants.AUDIT_FAILURE,
                    AuthConstants.REASON_INVALID_CREDENTIALS,
                    correlationId,
                    now
            );
            return new LoginAttempt(LoginOutcome.INVALID_CREDENTIALS, null);
        }

        if (user.isTemporarilyLocked(now) || user.getStatus() == UserStatus.LOCKED) {
            recordAudit(user.getId(), AuthConstants.AUDIT_FAILURE, AuthConstants.REASON_ACCOUNT_LOCKED, correlationId, now);
            return new LoginAttempt(LoginOutcome.ACCOUNT_LOCKED, null);
        }
        if (user.getStatus() == UserStatus.PENDING_VERIFICATION) {
            recordAudit(
                    user.getId(),
                    AuthConstants.AUDIT_FAILURE,
                    AuthConstants.REASON_EMAIL_NOT_VERIFIED,
                    correlationId,
                    now
            );
            return new LoginAttempt(LoginOutcome.EMAIL_NOT_VERIFIED, null);
        }
        if (user.getStatus() != UserStatus.ACTIVE) {
            recordAudit(null, AuthConstants.AUDIT_FAILURE, AuthConstants.REASON_INVALID_CREDENTIALS, correlationId, now);
            return new LoginAttempt(LoginOutcome.INVALID_CREDENTIALS, null);
        }

        user.recordSuccessfulLogin(now);
        recordAudit(user.getId(), AuthConstants.AUDIT_SUCCESS, null, correlationId, now);
        return new LoginAttempt(LoginOutcome.SUCCESS, user.getId());
    }

    @Transactional
    public void recordRateLimited(String correlationId) {
        recordAudit(
                null,
                AuthConstants.AUDIT_FAILURE,
                AuthConstants.REASON_RATE_LIMITED,
                correlationId,
                clock.instant()
        );
    }

    private void recordAudit(
            UUID subjectUserId,
            String outcome,
            String reasonCode,
            String correlationId,
            Instant occurredAt
    ) {
        auditEventRepository.save(new AuthAuditEvent(
                UUID.randomUUID(),
                AuthConstants.AUDIT_LOGIN,
                outcome,
                reasonCode,
                subjectUserId,
                correlationId,
                occurredAt
        ));
    }

    public record LoginAttempt(LoginOutcome outcome, UUID userId) {
    }

    public enum LoginOutcome {
        SUCCESS,
        INVALID_CREDENTIALS,
        EMAIL_NOT_VERIFIED,
        ACCOUNT_LOCKED
    }
}
