package com.memories.platform.auth.service;

import com.memories.platform.auth.constants.AuthConstants;
import com.memories.platform.auth.entity.AuthAuditEvent;
import com.memories.platform.auth.exception.PermissionDeniedException;
import com.memories.platform.auth.repository.AuthAuditEventRepository;
import com.memories.platform.auth.repository.UserRoleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

@Service
public class AuthorizationService {

    private final UserRoleRepository userRoleRepository;
    private final AuthAuditEventRepository auditEventRepository;
    private final Clock clock;
    private final CurrentActorService currentActorService;

    public AuthorizationService(
            UserRoleRepository userRoleRepository,
            AuthAuditEventRepository auditEventRepository,
            Clock clock,
            CurrentActorService currentActorService
    ) {
        this.userRoleRepository = userRoleRepository;
        this.auditEventRepository = auditEventRepository;
        this.clock = clock;
        this.currentActorService = currentActorService;
    }

    @Transactional(noRollbackFor = PermissionDeniedException.class)
    public UUID requirePermission(
            String permissionCode,
            String correlationId,
            String deniedEventType,
            UUID targetUserId
    ) {
        UUID actorId = currentActorService.userId();
        if (userRoleRepository.activeUserHasPermission(actorId, permissionCode)) {
            return actorId;
        }

        Instant now = clock.instant();
        auditEventRepository.save(new AuthAuditEvent(
                UUID.randomUUID(),
                deniedEventType,
                AuthConstants.AUDIT_FAILURE,
                AuthConstants.REASON_PERMISSION_DENIED,
                actorId,
                correlationId,
                now,
                actorId,
                targetUserId
        ));
        throw new PermissionDeniedException();
    }

}
