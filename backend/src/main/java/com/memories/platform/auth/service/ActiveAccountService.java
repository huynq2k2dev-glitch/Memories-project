package com.memories.platform.auth.service;

import com.memories.platform.auth.constants.AuthConstants;
import com.memories.platform.auth.dto.AccountSummaryResponse;
import com.memories.platform.auth.entity.AuthAuditEvent;
import com.memories.platform.auth.entity.UserAccount;
import com.memories.platform.auth.entity.UserStatus;
import com.memories.platform.auth.exception.UserAccountNotFoundException;
import com.memories.platform.auth.repository.AuthAuditEventRepository;
import com.memories.platform.auth.repository.UserAccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ActiveAccountService {

    private final UserAccountRepository userAccountRepository;
    private final AuthAuditEventRepository auditEventRepository;
    private final Clock clock;

    public ActiveAccountService(
            UserAccountRepository userAccountRepository,
            AuthAuditEventRepository auditEventRepository,
            Clock clock
    ) {
        this.userAccountRepository = userAccountRepository;
        this.auditEventRepository = auditEventRepository;
        this.clock = clock;
    }

    @Transactional
    public boolean isActive(UUID userId, String correlationId) {
        Optional<UserAccount> account = userAccountRepository.findById(userId);
        if (account.filter(UserAccount::isActive).isPresent()) {
            return true;
        }

        Instant now = clock.instant();
        UUID knownUserId = account.isPresent() ? userId : null;
        auditEventRepository.save(new AuthAuditEvent(
                UUID.randomUUID(),
                AuthConstants.AUDIT_ACCESS_DENIED,
                AuthConstants.AUDIT_FAILURE,
                AuthConstants.REASON_ACCOUNT_LOCKED,
                knownUserId,
                correlationId,
                now,
                knownUserId,
                knownUserId
        ));
        return false;
    }

    @Transactional(readOnly = true)
    public AccountSummaryResponse requireActiveByEmail(String rawEmail) {
        String email = rawEmail.trim().toLowerCase(Locale.ROOT);
        UserAccount account = userAccountRepository
                .findByEmailAndStatusAndDeletedAtIsNull(email, UserStatus.ACTIVE)
                .orElseThrow(UserAccountNotFoundException::new);
        return toSummary(account);
    }

    @Transactional(readOnly = true)
    public Map<UUID, AccountSummaryResponse> summaries(Collection<UUID> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }
        return userAccountRepository.findAllById(userIds).stream()
                .map(this::toSummary)
                .collect(Collectors.toUnmodifiableMap(
                        AccountSummaryResponse::id,
                        Function.identity()
                ));
    }

    private AccountSummaryResponse toSummary(UserAccount account) {
        return new AccountSummaryResponse(
                account.getId(),
                account.getDisplayName(),
                account.isActive()
        );
    }
}
