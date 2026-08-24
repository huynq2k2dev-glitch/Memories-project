package com.memories.platform.guest.service;

import com.memories.platform.audit.dto.AuditResult;
import com.memories.platform.audit.service.AuditLogService;
import com.memories.platform.guest.constants.GuestMessageConstants;
import com.memories.platform.guest.dto.CreateGuestMessageRequest;
import com.memories.platform.guest.dto.GuestMessageModerationResponse;
import com.memories.platform.guest.dto.GuestMessagePageResponse;
import com.memories.platform.guest.dto.ModerateGuestMessageRequest;
import com.memories.platform.guest.entity.GuestMessage;
import com.memories.platform.guest.entity.GuestMessageStatus;
import com.memories.platform.guest.exception.GuestMessageConflictException;
import com.memories.platform.guest.exception.GuestMessageNotFoundException;
import com.memories.platform.guest.exception.GuestMessageRateLimitException;
import com.memories.platform.guest.exception.GuestMessageTransitionException;
import com.memories.platform.guest.exception.InvalidGuestMessageQueryException;
import com.memories.platform.guest.repository.GuestMessageRepository;
import com.memories.platform.memory.dto.GuestMessageModeratorContextResponse;
import com.memories.platform.memory.dto.GuestMessageSubmissionContextResponse;
import com.memories.platform.memory.service.MemoryMessageAccessService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Service
public class GuestMessageService {

    private final GuestMessageRepository messageRepository;
    private final MemoryMessageAccessService memoryAccessService;
    private final GuestMessageSanitizationService sanitizationService;
    private final AuditLogService auditLogService;
    private final Clock clock;
    private final int rateLimitCount;
    private final Duration rateLimitWindow;

    public GuestMessageService(
            GuestMessageRepository messageRepository,
            MemoryMessageAccessService memoryAccessService,
            GuestMessageSanitizationService sanitizationService,
            AuditLogService auditLogService,
            Clock clock,
            @Value("${platform.guest-message.rate-limit-count}") int rateLimitCount,
            @Value("${platform.guest-message.rate-limit-window}") Duration rateLimitWindow
    ) {
        if (rateLimitCount < 1 || rateLimitWindow.isNegative() || rateLimitWindow.isZero()) {
            throw new IllegalArgumentException("Guest message rate limit must be positive");
        }
        this.messageRepository = messageRepository;
        this.memoryAccessService = memoryAccessService;
        this.sanitizationService = sanitizationService;
        this.auditLogService = auditLogService;
        this.clock = clock;
        this.rateLimitCount = rateLimitCount;
        this.rateLimitWindow = rateLimitWindow;
    }

    @Transactional
    public void submit(
            String slug,
            CreateGuestMessageRequest request,
            Map<String, String> cookies,
            String ipHash,
            String correlationId
    ) {
        GuestMessageSubmissionContextResponse context = memoryAccessService.requireSubmission(
                slug,
                cookies
        );
        Instant now = clock.instant();
        long recentMessages = messageRepository
                .countByMemoryIdAndIpHashAndCreatedAtGreaterThanEqual(
                        context.memoryId(),
                        ipHash,
                        now.minus(rateLimitWindow)
                );
        if (recentMessages >= rateLimitCount) {
            auditLogService.recordIsolatedWithMetadata(
                    null,
                    GuestMessageConstants.AUDIT_RATE_LIMIT_ACTION,
                    GuestMessageConstants.AUDIT_ENTITY_TYPE,
                    null,
                    AuditResult.DENIED,
                    correlationId,
                    Map.of("memoryId", context.memoryId())
            );
            throw new GuestMessageRateLimitException();
        }

        GuestMessageStatus initialStatus = context.moderationEnabled()
                ? GuestMessageStatus.PENDING
                : GuestMessageStatus.APPROVED;
        GuestMessage message = new GuestMessage(
                UUID.randomUUID(),
                context.memoryId(),
                sanitizationService.guestName(request.guestName()),
                sanitizationService.content(request.content()),
                initialStatus,
                ipHash,
                now
        );
        messageRepository.save(message);
        flush();
        auditLogService.recordWithMetadata(
                null,
                GuestMessageConstants.AUDIT_SUBMIT_ACTION,
                GuestMessageConstants.AUDIT_ENTITY_TYPE,
                message.getId(),
                AuditResult.SUCCESS,
                correlationId,
                Map.of(
                        "memoryId", context.memoryId(),
                        "initialStatus", initialStatus.name()
                )
        );
    }

    @Transactional(readOnly = true)
    public GuestMessagePageResponse list(
            UUID memoryId,
            GuestMessageStatus status,
            int page,
            int size
    ) {
        memoryAccessService.requireModerator(memoryId);
        if (page < 0 || size < 1 || size > GuestMessageConstants.MAX_PAGE_SIZE) {
            throw new InvalidGuestMessageQueryException();
        }
        PageRequest pageRequest = PageRequest.of(
                page,
                size,
                Sort.by("createdAt", "id").descending()
        );
        Page<GuestMessage> messages = status == null
                ? messageRepository.findAllByMemoryId(memoryId, pageRequest)
                : messageRepository.findAllByMemoryIdAndStatus(memoryId, status, pageRequest);
        return new GuestMessagePageResponse(
                messages.getContent().stream().map(this::toModerationResponse).toList(),
                messages.getNumber(),
                messages.getSize(),
                messages.getTotalElements(),
                messages.getTotalPages()
        );
    }

    @Transactional
    public GuestMessageModerationResponse moderate(
            UUID memoryId,
            UUID messageId,
            ModerateGuestMessageRequest request,
            String correlationId
    ) {
        GuestMessageModeratorContextResponse context = memoryAccessService.requireModerator(
                memoryId
        );
        GuestMessage message = messageRepository.findForUpdateByIdAndMemoryId(
                messageId,
                memoryId
        ).orElseThrow(GuestMessageNotFoundException::new);
        if (!message.canTransitionTo(request.status())) {
            throw new GuestMessageTransitionException();
        }

        message.moderate(request.status(), context.actorId(), clock.instant());
        flush();
        auditLogService.recordWithMetadata(
                context.actorId(),
                auditAction(request.status()),
                GuestMessageConstants.AUDIT_ENTITY_TYPE,
                message.getId(),
                AuditResult.SUCCESS,
                correlationId,
                Map.of(
                        "memoryId", memoryId,
                        "status", request.status().name()
                )
        );
        return toModerationResponse(message);
    }

    private String auditAction(GuestMessageStatus status) {
        return switch (status) {
            case APPROVED -> GuestMessageConstants.AUDIT_APPROVE_ACTION;
            case REJECTED -> GuestMessageConstants.AUDIT_REJECT_ACTION;
            case HIDDEN -> GuestMessageConstants.AUDIT_HIDE_ACTION;
            case PENDING -> GuestMessageConstants.AUDIT_REQUEUE_ACTION;
        };
    }

    private GuestMessageModerationResponse toModerationResponse(GuestMessage message) {
        return new GuestMessageModerationResponse(
                message.getId(),
                message.getGuestName(),
                message.getContent(),
                message.getStatus(),
                message.getModeratedBy(),
                message.getModeratedAt(),
                message.getCreatedAt(),
                message.getUpdatedAt()
        );
    }

    private void flush() {
        try {
            messageRepository.flush();
        } catch (DataIntegrityViolationException exception) {
            throw new GuestMessageConflictException();
        }
    }
}
