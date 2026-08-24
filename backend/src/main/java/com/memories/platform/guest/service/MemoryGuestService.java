package com.memories.platform.guest.service;

import com.memories.platform.guest.constants.MemoryGuestConstants;
import com.memories.platform.guest.dto.CreateMemoryGuestRequest;
import com.memories.platform.guest.dto.GuestAccessTokenResponse;
import com.memories.platform.guest.dto.GuestInvitationResponse;
import com.memories.platform.guest.dto.GuestRsvpResponse;
import com.memories.platform.guest.dto.MemoryGuestPageResponse;
import com.memories.platform.guest.dto.MemoryGuestResponse;
import com.memories.platform.guest.dto.MemoryGuestVersionRequest;
import com.memories.platform.guest.dto.ShareLinkGuestResponse;
import com.memories.platform.guest.dto.SubmitGuestRsvpRequest;
import com.memories.platform.guest.dto.UpdateMemoryGuestRequest;
import com.memories.platform.guest.entity.MemoryGuest;
import com.memories.platform.guest.entity.MemoryGuestStatus;
import com.memories.platform.guest.entity.GuestAttendanceStatus;
import com.memories.platform.guest.entity.GuestEventResponse;
import com.memories.platform.guest.exception.GuestRsvpConflictException;
import com.memories.platform.guest.exception.GuestRsvpNotFoundException;
import com.memories.platform.guest.exception.GuestRsvpVersionConflictException;
import com.memories.platform.guest.exception.GuestInvitationNotFoundException;
import com.memories.platform.guest.exception.InvalidMemoryGuestQueryException;
import com.memories.platform.guest.exception.MemoryGuestConflictException;
import com.memories.platform.guest.exception.MemoryGuestNotActiveException;
import com.memories.platform.guest.exception.MemoryGuestNotFoundException;
import com.memories.platform.guest.exception.MemoryGuestVersionConflictException;
import com.memories.platform.guest.exception.InvalidGuestRsvpException;
import com.memories.platform.guest.repository.GuestEventResponseRepository;
import com.memories.platform.guest.repository.MemoryGuestRepository;
import com.memories.platform.memory.dto.GuestMemoryContextResponse;
import com.memories.platform.memory.exception.MemoryNotFoundException;
import com.memories.platform.memory.service.MemoryAccessService;
import com.memories.platform.memory.service.MemoryContentSafetyService;
import com.memories.platform.memory.service.MemoryGuestInvitationService;
import com.memories.platform.utils.TokenHashUtils;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class MemoryGuestService {

    private final MemoryGuestRepository guestRepository;
    private final GuestEventResponseRepository responseRepository;
    private final MemoryAccessService memoryAccessService;
    private final MemoryGuestInvitationService invitationService;
    private final MemoryContentSafetyService contentSafetyService;
    private final SecureRandom secureRandom;
    private final Clock clock;

    public MemoryGuestService(
            MemoryGuestRepository guestRepository,
            GuestEventResponseRepository responseRepository,
            MemoryAccessService memoryAccessService,
            MemoryGuestInvitationService invitationService,
            MemoryContentSafetyService contentSafetyService,
            SecureRandom secureRandom,
            Clock clock
    ) {
        this.guestRepository = guestRepository;
        this.responseRepository = responseRepository;
        this.memoryAccessService = memoryAccessService;
        this.invitationService = invitationService;
        this.contentSafetyService = contentSafetyService;
        this.secureRandom = secureRandom;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public MemoryGuestPageResponse list(UUID memoryId, int page, int size) {
        memoryAccessService.requireOwner(memoryId);
        if (page < 0 || size < 1 || size > MemoryGuestConstants.MAX_PAGE_SIZE) {
            throw new InvalidMemoryGuestQueryException();
        }
        Page<MemoryGuest> guests = guestRepository.findAllByMemoryId(
                memoryId,
                PageRequest.of(page, size, Sort.by("createdAt", "id").descending())
        );
        return new MemoryGuestPageResponse(
                guests.getContent().stream().map(this::toResponse).toList(),
                guests.getNumber(),
                guests.getSize(),
                guests.getTotalElements(),
                guests.getTotalPages()
        );
    }

    @Transactional
    public MemoryGuestResponse create(UUID memoryId, CreateMemoryGuestRequest request) {
        memoryAccessService.requireOwner(memoryId);
        UUID actorId = memoryAccessService.actorId();
        GuestValues values = values(
                request.fullName(),
                request.email(),
                request.phone(),
                request.guestGroup(),
                request.maxPartySize(),
                request.note()
        );
        Instant now = clock.instant();
        MemoryGuest guest = new MemoryGuest(
                UUID.randomUUID(),
                memoryId,
                values.fullName(),
                values.email(),
                values.phone(),
                values.guestGroup(),
                values.maxPartySize(),
                values.note(),
                actorId,
                now
        );
        guestRepository.save(guest);
        flush();
        return toResponse(guest);
    }

    @Transactional
    public MemoryGuestResponse update(
            UUID memoryId,
            UUID guestId,
            UpdateMemoryGuestRequest request
    ) {
        memoryAccessService.requireOwner(memoryId);
        MemoryGuest guest = requireForUpdate(memoryId, guestId);
        requireVersion(guest, request.version());
        GuestValues values = values(
                request.fullName(),
                request.email(),
                request.phone(),
                request.guestGroup(),
                request.maxPartySize(),
                request.note()
        );
        guest.update(
                values.fullName(),
                values.email(),
                values.phone(),
                values.guestGroup(),
                values.maxPartySize(),
                values.note(),
                memoryAccessService.actorId(),
                clock.instant()
        );
        flush();
        return toResponse(guest);
    }

    @Transactional
    public GuestAccessTokenResponse issueAccessToken(
            UUID memoryId,
            UUID guestId,
            MemoryGuestVersionRequest request
    ) {
        memoryAccessService.requireOwner(memoryId);
        MemoryGuest guest = requireForUpdate(memoryId, guestId);
        requireVersion(guest, request.version());
        if (!guest.isActive()) {
            throw new MemoryGuestNotActiveException();
        }

        String accessToken = accessToken();
        Instant now = clock.instant();
        guest.issueAccessToken(
                TokenHashUtils.sha256(accessToken),
                memoryAccessService.actorId(),
                now
        );
        flush();
        return new GuestAccessTokenResponse(
                guest.getId(),
                accessToken,
                MemoryGuestConstants.INVITATION_PATH_PREFIX + accessToken,
                now,
                guest.getVersion()
        );
    }

    @Transactional
    public void disable(
            UUID memoryId,
            UUID guestId,
            MemoryGuestVersionRequest request
    ) {
        memoryAccessService.requireOwner(memoryId);
        MemoryGuest guest = requireForUpdate(memoryId, guestId);
        requireVersion(guest, request.version());
        if (!guest.isActive()) {
            throw new MemoryGuestNotActiveException();
        }
        guest.disable(memoryAccessService.actorId(), clock.instant());
        flush();
    }

    @Transactional(readOnly = true)
    public GuestInvitationResponse invitation(String accessToken) {
        MemoryGuest guest = requireActiveGuest(accessToken);
        return invitation(guest);
    }

    @Transactional(readOnly = true)
    public List<ShareLinkGuestResponse> activeShareGuests(UUID memoryId) {
        return guestRepository.findAllByMemoryIdAndStatusOrderByFullNameAsc(
                memoryId,
                MemoryGuestStatus.ACTIVE
        ).stream().map(guest -> new ShareLinkGuestResponse(
                guest.getId(),
                guest.getFullName(),
                guest.getGuestGroup(),
                guest.getMaxPartySize()
        )).toList();
    }

    @Transactional(readOnly = true)
    public boolean isActiveShareGuest(UUID memoryId, UUID guestId) {
        return guestId != null && guestRepository.existsByIdAndMemoryIdAndStatus(
                guestId,
                memoryId,
                MemoryGuestStatus.ACTIVE
        );
    }

    @Transactional(readOnly = true)
    public GuestInvitationResponse shareInvitation(UUID memoryId, UUID guestId) {
        return invitation(requireActiveShareGuest(memoryId, guestId));
    }

    private GuestInvitationResponse invitation(MemoryGuest guest) {
        GuestMemoryContextResponse context;
        try {
            context = invitationService.get(guest.getMemoryId());
        } catch (MemoryNotFoundException exception) {
            throw new GuestInvitationNotFoundException();
        }
        Map<UUID, GuestEventResponse> responses = context.events().isEmpty()
                ? Map.of()
                : responseRepository.findAllByGuestIdAndEventIdIn(
                        guest.getId(),
                        context.events().stream().map(GuestMemoryContextResponse.Event::id).toList()
                ).stream().collect(Collectors.toUnmodifiableMap(
                        GuestEventResponse::getEventId,
                        Function.identity()
                ));
        return new GuestInvitationResponse(
                new GuestInvitationResponse.Guest(
                        guest.getFullName(),
                        guest.getGuestGroup(),
                        guest.getMaxPartySize()
                ),
                new GuestInvitationResponse.Memory(context.title()),
                context.events().stream().map(event -> new GuestInvitationResponse.Event(
                        event.id(),
                        event.eventType(),
                        event.title(),
                        event.description(),
                        event.startAt(),
                        event.endAt(),
                        event.timezone(),
                        event.sortOrder(),
                        toInvitationRsvp(responses.get(event.id()))
                )).toList()
        );
    }

    @Transactional
    public GuestRsvpResponse respond(
            String accessToken,
            SubmitGuestRsvpRequest request
    ) {
        return respond(requireActiveGuest(accessToken), request);
    }

    @Transactional
    public GuestRsvpResponse respondToShare(
            UUID memoryId,
            UUID guestId,
            SubmitGuestRsvpRequest request
    ) {
        return respond(requireActiveShareGuest(memoryId, guestId), request);
    }

    private GuestRsvpResponse respond(
            MemoryGuest guest,
            SubmitGuestRsvpRequest request
    ) {
        try {
            invitationService.requireRsvpEvent(guest.getMemoryId(), request.eventId());
        } catch (MemoryNotFoundException exception) {
            throw new GuestRsvpNotFoundException();
        }

        requireValidPartySize(
                request.attendanceStatus(),
                request.partySize(),
                guest.getMaxPartySize()
        );
        String dietaryNote = trimToNull(request.dietaryNote());
        String message = trimToNull(request.message());
        contentSafetyService.requireSafeMarkdown(dietaryNote);
        contentSafetyService.requireSafeMarkdown(message);

        Instant now = clock.instant();
        Instant respondedAt = request.attendanceStatus() == GuestAttendanceStatus.PENDING
                ? null
                : now;
        GuestEventResponse response = responseRepository
                .findForUpdateByGuestIdAndEventId(guest.getId(), request.eventId())
                .orElse(null);
        if (response == null) {
            if (request.version() != null) {
                throw new GuestRsvpVersionConflictException(null);
            }
            response = new GuestEventResponse(
                    UUID.randomUUID(),
                    guest.getId(),
                    request.eventId(),
                    request.attendanceStatus(),
                    request.partySize(),
                    dietaryNote,
                    message,
                    respondedAt,
                    now
            );
            responseRepository.save(response);
        } else {
            requireRsvpVersion(response, request.version());
            response.update(
                    request.attendanceStatus(),
                    request.partySize(),
                    dietaryNote,
                    message,
                    respondedAt,
                    now
            );
        }
        flushRsvp();
        return toRsvpResponse(response);
    }

    private MemoryGuest requireActiveGuest(String accessToken) {
        if (accessToken == null
                || accessToken.length() != MemoryGuestConstants.ACCESS_TOKEN_LENGTH) {
            throw new GuestInvitationNotFoundException();
        }
        return guestRepository.findByAccessTokenHashAndStatus(
                TokenHashUtils.sha256(accessToken),
                MemoryGuestStatus.ACTIVE
        ).orElseThrow(GuestInvitationNotFoundException::new);
    }

    private MemoryGuest requireActiveShareGuest(UUID memoryId, UUID guestId) {
        return guestRepository.findByIdAndMemoryIdAndStatus(
                guestId,
                memoryId,
                MemoryGuestStatus.ACTIVE
        ).orElseThrow(GuestInvitationNotFoundException::new);
    }

    private void requireValidPartySize(
            GuestAttendanceStatus status,
            int partySize,
            int maximumPartySize
    ) {
        boolean valid = status == GuestAttendanceStatus.DECLINED
                ? partySize == 0
                : partySize >= 1 && partySize <= maximumPartySize;
        if (!valid) {
            throw new InvalidGuestRsvpException();
        }
    }

    private void requireRsvpVersion(GuestEventResponse response, Long expectedVersion) {
        if (expectedVersion == null || response.getVersion() != expectedVersion) {
            throw new GuestRsvpVersionConflictException(response.getVersion());
        }
    }

    private MemoryGuest requireForUpdate(UUID memoryId, UUID guestId) {
        return guestRepository.findForUpdateByIdAndMemoryId(guestId, memoryId)
                .orElseThrow(MemoryGuestNotFoundException::new);
    }

    private void requireVersion(MemoryGuest guest, Long expectedVersion) {
        if (expectedVersion == null || guest.getVersion() != expectedVersion) {
            throw new MemoryGuestVersionConflictException(guest.getVersion());
        }
    }

    private GuestValues values(
            String fullName,
            String email,
            String phone,
            String guestGroup,
            Integer maxPartySize,
            String note
    ) {
        String normalizedFullName = fullName.trim();
        String normalizedGroup = trimToNull(guestGroup);
        String normalizedNote = trimToNull(note);
        contentSafetyService.requireSafeMarkdown(normalizedFullName);
        contentSafetyService.requireSafeMarkdown(normalizedGroup);
        contentSafetyService.requireSafeMarkdown(normalizedNote);
        return new GuestValues(
                normalizedFullName,
                trimToNull(email),
                trimToNull(phone),
                normalizedGroup,
                maxPartySize,
                normalizedNote
        );
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private String accessToken() {
        byte[] bytes = new byte[MemoryGuestConstants.ACCESS_TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private void flush() {
        try {
            guestRepository.flush();
        } catch (ObjectOptimisticLockingFailureException exception) {
            throw new MemoryGuestVersionConflictException(null);
        } catch (DataIntegrityViolationException exception) {
            throw new MemoryGuestConflictException();
        }
    }

    private void flushRsvp() {
        try {
            responseRepository.flush();
        } catch (ObjectOptimisticLockingFailureException exception) {
            throw new GuestRsvpVersionConflictException(null);
        } catch (DataIntegrityViolationException exception) {
            throw new GuestRsvpConflictException();
        }
    }

    private GuestInvitationResponse.Rsvp toInvitationRsvp(GuestEventResponse response) {
        if (response == null) {
            return null;
        }
        return new GuestInvitationResponse.Rsvp(
                response.getAttendanceStatus(),
                response.getPartySize(),
                response.getDietaryNote(),
                response.getMessage(),
                response.getRespondedAt(),
                response.getUpdatedAt(),
                response.getVersion()
        );
    }

    private GuestRsvpResponse toRsvpResponse(GuestEventResponse response) {
        return new GuestRsvpResponse(
                response.getEventId(),
                response.getAttendanceStatus(),
                response.getPartySize(),
                response.getDietaryNote(),
                response.getMessage(),
                response.getRespondedAt(),
                response.getUpdatedAt(),
                response.getVersion()
        );
    }

    private MemoryGuestResponse toResponse(MemoryGuest guest) {
        return new MemoryGuestResponse(
                guest.getId(),
                guest.getFullName(),
                guest.getEmail(),
                guest.getPhone(),
                guest.getGuestGroup(),
                guest.getMaxPartySize(),
                guest.getNote(),
                guest.getStatus(),
                guest.hasAccessToken(),
                guest.getCreatedAt(),
                guest.getUpdatedAt(),
                guest.getVersion()
        );
    }

    private record GuestValues(
            String fullName,
            String email,
            String phone,
            String guestGroup,
            int maxPartySize,
            String note
    ) {
    }
}
