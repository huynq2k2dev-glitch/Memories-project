package com.memories.platform.memory.service;

import com.memories.platform.audit.dto.AuditResult;
import com.memories.platform.audit.service.AuditLogService;
import com.memories.platform.auth.dto.AccountSummaryResponse;
import com.memories.platform.auth.service.ActiveAccountService;
import com.memories.platform.memory.constants.MemoryCollaboratorConstants;
import com.memories.platform.memory.dto.AddMemoryCollaboratorRequest;
import com.memories.platform.memory.dto.MemoryCollaboratorResponse;
import com.memories.platform.memory.dto.UpdateMemoryCollaboratorRequest;
import com.memories.platform.memory.entity.Memory;
import com.memories.platform.memory.entity.MemoryCollaborator;
import com.memories.platform.memory.exception.MemoryCollaboratorConflictException;
import com.memories.platform.memory.exception.MemoryCollaboratorNotFoundException;
import com.memories.platform.memory.repository.MemoryCollaboratorRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class MemoryCollaborationPersistenceService {

    private final MemoryCollaboratorRepository collaboratorRepository;
    private final MemoryAccessService accessService;
    private final ActiveAccountService activeAccountService;
    private final AuditLogService auditLogService;
    private final Clock clock;

    public MemoryCollaborationPersistenceService(
            MemoryCollaboratorRepository collaboratorRepository,
            MemoryAccessService accessService,
            ActiveAccountService activeAccountService,
            AuditLogService auditLogService,
            Clock clock
    ) {
        this.collaboratorRepository = collaboratorRepository;
        this.accessService = accessService;
        this.activeAccountService = activeAccountService;
        this.auditLogService = auditLogService;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<MemoryCollaboratorResponse> list(UUID memoryId) {
        accessService.requireManageCollaborators(memoryId);
        List<MemoryCollaborator> collaborators = collaboratorRepository
                .findAllByMemoryIdOrderByCreatedAtAsc(memoryId);
        Set<UUID> userIds = collaborators.stream()
                .map(MemoryCollaborator::getUserId)
                .collect(Collectors.toUnmodifiableSet());
        Map<UUID, AccountSummaryResponse> accounts = activeAccountService.summaries(userIds);
        return collaborators.stream()
                .map(collaborator -> toResponse(
                        collaborator,
                        accounts.get(collaborator.getUserId())
                ))
                .toList();
    }

    @Transactional
    public MemoryCollaboratorResponse add(
            UUID memoryId,
            UUID actorId,
            AddMemoryCollaboratorRequest request,
            String correlationId
    ) {
        Memory memory = accessService.requireManageCollaborators(memoryId);
        AccountSummaryResponse account = activeAccountService.requireActiveByEmail(request.email());
        if (account.id().equals(actorId) || account.id().equals(memory.getOwnerId())) {
            throw new MemoryCollaboratorConflictException();
        }

        Instant now = clock.instant();
        MemoryCollaborator collaborator;
        boolean reactivated;
        var existing = collaboratorRepository.findForUpdateByMemoryIdAndUserId(
                memoryId,
                account.id()
        );
        if (existing.isPresent()) {
            collaborator = existing.get();
            if (collaborator.isActive()) {
                throw new MemoryCollaboratorConflictException();
            }
            collaborator.reactivate(request.permission(), actorId, now);
            reactivated = true;
        } else {
            collaborator = new MemoryCollaborator(
                    UUID.randomUUID(),
                    memoryId,
                    account.id(),
                    request.permission(),
                    actorId,
                    now
            );
            collaboratorRepository.save(collaborator);
            reactivated = false;
        }

        flush();
        auditLogService.recordWithMetadata(
                actorId,
                MemoryCollaboratorConstants.AUDIT_ADD_ACTION,
                MemoryCollaboratorConstants.AUDIT_ENTITY_TYPE,
                collaborator.getId(),
                AuditResult.SUCCESS,
                correlationId,
                Map.of(
                        "memoryId", memoryId,
                        "targetUserId", account.id(),
                        "permission", request.permission().name(),
                        "reactivated", reactivated
                )
        );
        return toResponse(collaborator, account);
    }

    @Transactional
    public MemoryCollaboratorResponse changePermission(
            UUID memoryId,
            UUID collaboratorId,
            UUID actorId,
            UpdateMemoryCollaboratorRequest request,
            String correlationId
    ) {
        accessService.requireManageCollaborators(memoryId);
        MemoryCollaborator collaborator = requireActive(memoryId, collaboratorId);
        collaborator.changePermission(request.permission(), clock.instant());
        flush();
        auditLogService.recordWithMetadata(
                actorId,
                MemoryCollaboratorConstants.AUDIT_PERMISSION_ACTION,
                MemoryCollaboratorConstants.AUDIT_ENTITY_TYPE,
                collaborator.getId(),
                AuditResult.SUCCESS,
                correlationId,
                Map.of(
                        "memoryId", memoryId,
                        "targetUserId", collaborator.getUserId(),
                        "permission", request.permission().name()
                )
        );
        AccountSummaryResponse account = activeAccountService.summaries(
                Set.of(collaborator.getUserId())
        ).get(collaborator.getUserId());
        return toResponse(collaborator, account);
    }

    @Transactional
    public void revoke(
            UUID memoryId,
            UUID collaboratorId,
            UUID actorId,
            String correlationId
    ) {
        accessService.requireManageCollaborators(memoryId);
        MemoryCollaborator collaborator = requireActive(memoryId, collaboratorId);
        collaborator.revoke(clock.instant());
        flush();
        auditLogService.recordWithMetadata(
                actorId,
                MemoryCollaboratorConstants.AUDIT_REVOKE_ACTION,
                MemoryCollaboratorConstants.AUDIT_ENTITY_TYPE,
                collaborator.getId(),
                AuditResult.SUCCESS,
                correlationId,
                Map.of(
                        "memoryId", memoryId,
                        "targetUserId", collaborator.getUserId(),
                        "permission", collaborator.getPermission().name()
                )
        );
    }

    private MemoryCollaborator requireActive(UUID memoryId, UUID collaboratorId) {
        MemoryCollaborator collaborator = collaboratorRepository
                .findForUpdateByIdAndMemoryId(collaboratorId, memoryId)
                .orElseThrow(MemoryCollaboratorNotFoundException::new);
        if (!collaborator.isActive()) {
            throw new MemoryCollaboratorNotFoundException();
        }
        return collaborator;
    }

    private void flush() {
        try {
            collaboratorRepository.flush();
        } catch (DataIntegrityViolationException exception) {
            throw new MemoryCollaboratorConflictException();
        }
    }

    private MemoryCollaboratorResponse toResponse(
            MemoryCollaborator collaborator,
            AccountSummaryResponse account
    ) {
        return new MemoryCollaboratorResponse(
                collaborator.getId(),
                collaborator.getUserId(),
                account == null ? null : account.displayName(),
                account != null && account.active(),
                collaborator.getPermission(),
                collaborator.getStatus(),
                collaborator.getInvitedBy(),
                collaborator.getCreatedAt(),
                collaborator.getUpdatedAt(),
                collaborator.getRevokedAt()
        );
    }
}
