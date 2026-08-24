package com.memories.platform.memory.service;

import com.memories.platform.audit.dto.AuditResult;
import com.memories.platform.audit.service.AuditLogService;
import com.memories.platform.guest.dto.GuestInvitationResponse;
import com.memories.platform.guest.dto.GuestRsvpResponse;
import com.memories.platform.guest.dto.ShareLinkGuestResponse;
import com.memories.platform.guest.dto.SubmitGuestRsvpRequest;
import com.memories.platform.guest.exception.GuestInvitationNotFoundException;
import com.memories.platform.guest.service.MemoryGuestService;
import com.memories.platform.memory.constants.MemoryPublishingConstants;
import com.memories.platform.memory.constants.ShareLinkConstants;
import com.memories.platform.memory.dto.CreateShareLinkRequest;
import com.memories.platform.memory.dto.IssuedShareLinkResponse;
import com.memories.platform.memory.dto.ShareLinkRedemptionResult;
import com.memories.platform.memory.dto.ShareLinkResponse;
import com.memories.platform.memory.entity.Memory;
import com.memories.platform.memory.entity.MemoryStatus;
import com.memories.platform.memory.entity.MemoryVisibility;
import com.memories.platform.memory.entity.ShareLink;
import com.memories.platform.memory.entity.ShareLinkPermission;
import com.memories.platform.memory.entity.ShareLinkStatus;
import com.memories.platform.memory.exception.InvalidShareLinkException;
import com.memories.platform.memory.exception.ShareLinkConflictException;
import com.memories.platform.memory.exception.ShareLinkNotFoundException;
import com.memories.platform.memory.repository.MemoryRepository;
import com.memories.platform.memory.repository.ShareLinkRepository;
import com.memories.platform.utils.TokenHashUtils;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class MemoryShareLinkService {

    private final ShareLinkRepository shareLinkRepository;
    private final MemoryRepository memoryRepository;
    private final MemoryAccessService memoryAccessService;
    private final MemoryAccessCookieService cookieService;
    private final MemoryGuestService guestService;
    private final AuditLogService auditLogService;
    private final SecureRandom secureRandom;
    private final Clock clock;

    public MemoryShareLinkService(
            ShareLinkRepository shareLinkRepository,
            MemoryRepository memoryRepository,
            MemoryAccessService memoryAccessService,
            MemoryAccessCookieService cookieService,
            MemoryGuestService guestService,
            AuditLogService auditLogService,
            SecureRandom secureRandom,
            Clock clock
    ) {
        this.shareLinkRepository = shareLinkRepository;
        this.memoryRepository = memoryRepository;
        this.memoryAccessService = memoryAccessService;
        this.cookieService = cookieService;
        this.guestService = guestService;
        this.auditLogService = auditLogService;
        this.secureRandom = secureRandom;
        this.clock = clock;
    }

    @Transactional
    public List<ShareLinkResponse> list(UUID memoryId) {
        memoryAccessService.requireManageCollaborators(memoryId);
        expireLinks(memoryId);
        return shareLinkRepository.findAllByMemoryIdOrderByCreatedAtDesc(memoryId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ShareLinkGuestResponse> eligibleGuests(UUID memoryId) {
        memoryAccessService.requireManageCollaborators(memoryId);
        return guestService.activeShareGuests(memoryId);
    }

    @Transactional
    public IssuedShareLinkResponse issue(
            UUID memoryId,
            CreateShareLinkRequest request,
            String correlationId
    ) {
        Memory memory = memoryAccessService.requireManageCollaborators(memoryId);
        requireShareable(memory);

        Instant now = clock.instant();
        requireValidRequest(memoryId, request, now);
        UUID actorId = memoryAccessService.actorId();
        String accessToken = token();
        ShareLink shareLink = new ShareLink(
                UUID.randomUUID(),
                memoryId,
                TokenHashUtils.sha256(accessToken),
                request.permission(),
                request.guestId(),
                request.expiresAt(),
                request.maxUses(),
                actorId,
                now
        );
        shareLinkRepository.save(shareLink);
        flush();

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("memoryId", memoryId);
        metadata.put("permission", request.permission().name());
        if (request.guestId() != null) {
            metadata.put("guestId", request.guestId());
        }
        if (request.expiresAt() != null) {
            metadata.put("expiresAt", request.expiresAt());
        }
        if (request.maxUses() != null) {
            metadata.put("maxUses", request.maxUses());
        }
        auditLogService.recordWithMetadata(
                actorId,
                ShareLinkConstants.AUDIT_CREATE_ACTION,
                ShareLinkConstants.AUDIT_ENTITY_TYPE,
                shareLink.getId(),
                AuditResult.SUCCESS,
                correlationId,
                metadata
        );
        return new IssuedShareLinkResponse(
                toResponse(shareLink),
                accessToken,
                ShareLinkConstants.PUBLIC_PATH_PREFIX + accessToken
        );
    }

    @Transactional
    public void revoke(UUID memoryId, UUID shareLinkId, String correlationId) {
        memoryAccessService.requireManageCollaborators(memoryId);
        expireLinks(memoryId);
        ShareLink shareLink = shareLinkRepository.findForUpdateByIdAndMemoryId(
                shareLinkId,
                memoryId
        ).orElseThrow(ShareLinkNotFoundException::new);
        if (shareLink.getStatus() != ShareLinkStatus.ACTIVE) {
            throw new ShareLinkNotFoundException();
        }

        Instant now = clock.instant();
        shareLink.revoke(now);
        flush();
        auditLogService.recordWithMetadata(
                memoryAccessService.actorId(),
                ShareLinkConstants.AUDIT_REVOKE_ACTION,
                ShareLinkConstants.AUDIT_ENTITY_TYPE,
                shareLink.getId(),
                AuditResult.SUCCESS,
                correlationId,
                Map.of(
                        "memoryId", memoryId,
                        "permission", shareLink.getPermission().name(),
                        "useCount", shareLink.getUseCount()
                )
        );
    }

    @Transactional
    public ShareLinkRedemptionResult redeem(String accessToken) {
        requireTokenShape(accessToken);
        ShareLink shareLink = shareLinkRepository.findForUpdateByTokenHash(
                TokenHashUtils.sha256(accessToken)
        ).orElseThrow(ShareLinkNotFoundException::new);
        Instant now = clock.instant();
        Memory memory = memoryRepository.findByIdAndDeletedAtIsNull(shareLink.getMemoryId())
                .orElseThrow(ShareLinkNotFoundException::new);
        boolean memoryAvailable = memory.getStatus() == MemoryStatus.PUBLISHED
                && isShareable(memory)
                && (memory.getExpiresAt() == null || memory.getExpiresAt().isAfter(now));
        boolean guestAvailable = shareLink.getPermission() == ShareLinkPermission.VIEW
                || guestService.isActiveShareGuest(
                        memory.getId(),
                        shareLink.getGuestId()
                );
        if (!memoryAvailable || !guestAvailable || !shareLink.redeem(now)) {
            throw new ShareLinkNotFoundException();
        }
        flush();
        return new ShareLinkRedemptionResult(
                memory.getId(),
                memory.getSlug(),
                shareLink.getPermission(),
                shareLink.getExpiresAt()
        );
    }

    public String accessCookie(
            ShareLinkRedemptionResult redemption,
            String accessToken
    ) {
        if (redemption.expiresAt() == null) {
            return cookieService.createSession(redemption.memoryId(), accessToken);
        }
        return cookieService.create(
                redemption.memoryId(),
                accessToken,
                redemption.expiresAt()
        );
    }

    @Transactional(readOnly = true)
    public boolean hasValidViewGrant(UUID memoryId, Map<String, String> cookies) {
        ShareLink shareLink = validGrant(memoryId, cookies);
        return shareLink != null && (
                shareLink.getPermission() == ShareLinkPermission.VIEW
                        || guestService.isActiveShareGuest(memoryId, shareLink.getGuestId())
        );
    }

    @Transactional(readOnly = true)
    public GuestInvitationResponse rsvpInvitation(
            String slug,
            Map<String, String> cookies
    ) {
        GrantedRsvp granted = requireRsvpGrant(slug, cookies);
        try {
            return guestService.shareInvitation(granted.memoryId(), granted.guestId());
        } catch (GuestInvitationNotFoundException exception) {
            throw new ShareLinkNotFoundException();
        }
    }

    @Transactional
    public GuestRsvpResponse respond(
            String slug,
            Map<String, String> cookies,
            SubmitGuestRsvpRequest request
    ) {
        GrantedRsvp granted = requireRsvpGrant(slug, cookies);
        try {
            return guestService.respondToShare(
                    granted.memoryId(),
                    granted.guestId(),
                    request
            );
        } catch (GuestInvitationNotFoundException exception) {
            throw new ShareLinkNotFoundException();
        }
    }

    private GrantedRsvp requireRsvpGrant(String slug, Map<String, String> cookies) {
        Memory memory = memoryRepository.findPublicBySlug(
                slug,
                MemoryStatus.PUBLISHED,
                MemoryPublishingConstants.PUBLISHABLE_VISIBILITIES,
                clock.instant()
        ).orElseThrow(ShareLinkNotFoundException::new);
        if (!isShareable(memory)) {
            throw new ShareLinkNotFoundException();
        }
        ShareLink shareLink = validGrant(memory.getId(), cookies);
        if (shareLink == null
                || shareLink.getPermission() != ShareLinkPermission.RSVP
                || !guestService.isActiveShareGuest(memory.getId(), shareLink.getGuestId())) {
            throw new ShareLinkNotFoundException();
        }
        return new GrantedRsvp(memory.getId(), shareLink.getGuestId());
    }

    private ShareLink validGrant(UUID memoryId, Map<String, String> cookies) {
        String accessToken = cookieService.token(cookies, memoryId);
        if (!validTokenShape(accessToken)) {
            return null;
        }
        return shareLinkRepository.findValidGrant(
                memoryId,
                TokenHashUtils.sha256(accessToken),
                ShareLinkStatus.ACTIVE,
                clock.instant()
        ).orElse(null);
    }

    private void requireValidRequest(
            UUID memoryId,
            CreateShareLinkRequest request,
            Instant now
    ) {
        if (request.expiresAt() != null && !request.expiresAt().isAfter(now)) {
            throw new InvalidShareLinkException();
        }
        if (request.permission() == ShareLinkPermission.VIEW && request.guestId() != null) {
            throw new InvalidShareLinkException();
        }
        if (request.permission() == ShareLinkPermission.RSVP
                && !guestService.isActiveShareGuest(memoryId, request.guestId())) {
            throw new InvalidShareLinkException();
        }
    }

    private void requireShareable(Memory memory) {
        if (!isShareable(memory)) {
            throw new InvalidShareLinkException();
        }
    }

    private boolean isShareable(Memory memory) {
        return memory.getVisibility() == MemoryVisibility.PRIVATE
                || memory.getVisibility() == MemoryVisibility.UNLISTED;
    }

    private void expireLinks(UUID memoryId) {
        shareLinkRepository.expireActiveByMemoryId(
                memoryId,
                clock.instant(),
                ShareLinkStatus.ACTIVE,
                ShareLinkStatus.EXPIRED
        );
    }

    private void flush() {
        try {
            shareLinkRepository.flush();
        } catch (DataIntegrityViolationException exception) {
            throw new ShareLinkConflictException();
        }
    }

    private String token() {
        byte[] bytes = new byte[ShareLinkConstants.TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private void requireTokenShape(String accessToken) {
        if (!validTokenShape(accessToken)) {
            throw new ShareLinkNotFoundException();
        }
    }

    private boolean validTokenShape(String accessToken) {
        return accessToken != null && accessToken.length() == ShareLinkConstants.TOKEN_LENGTH;
    }

    private ShareLinkResponse toResponse(ShareLink shareLink) {
        return new ShareLinkResponse(
                shareLink.getId(),
                shareLink.getPermission(),
                shareLink.getGuestId(),
                shareLink.getExpiresAt(),
                shareLink.getMaxUses(),
                shareLink.getUseCount(),
                shareLink.getStatus(),
                shareLink.getCreatedBy(),
                shareLink.getCreatedAt(),
                shareLink.getRevokedAt()
        );
    }

    private record GrantedRsvp(UUID memoryId, UUID guestId) {
    }
}
