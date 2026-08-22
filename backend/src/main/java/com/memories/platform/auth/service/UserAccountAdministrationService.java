package com.memories.platform.auth.service;

import com.memories.platform.auth.constants.AuthConstants;
import com.memories.platform.auth.dto.AccountStatusResponse;
import com.memories.platform.auth.exception.UserAccountNotFoundException;
import com.memories.platform.auth.service.UserAccountLockPersistenceService.LockResult;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class UserAccountAdministrationService {

    private final AuthorizationService authorizationService;
    private final UserAccountLockPersistenceService lockPersistenceService;

    public UserAccountAdministrationService(
            AuthorizationService authorizationService,
            UserAccountLockPersistenceService lockPersistenceService
    ) {
        this.authorizationService = authorizationService;
        this.lockPersistenceService = lockPersistenceService;
    }

    public AccountStatusResponse lock(UUID targetUserId, String correlationId) {
        UUID actorId = authorizationService.requirePermission(
                AuthConstants.PERMISSION_USER_MANAGE,
                correlationId,
                AuthConstants.AUDIT_ACCOUNT_LOCK,
                targetUserId
        );
        LockResult result = lockPersistenceService.lock(actorId, targetUserId, correlationId);
        if (result.outcome() == UserAccountLockPersistenceService.LockOutcome.NOT_FOUND) {
            throw new UserAccountNotFoundException();
        }
        return new AccountStatusResponse(result.userId(), "LOCKED");
    }
}
