package com.memories.platform.memory.service;

import com.memories.platform.memory.dto.CreateMemoryEventRequest;
import com.memories.platform.memory.dto.MemoryEventResponse;
import com.memories.platform.memory.dto.ReorderMemoryItemsRequest;
import com.memories.platform.memory.dto.UpdateMemoryEventRequest;
import com.memories.platform.memory.entity.MemoryEvent;
import com.memories.platform.memory.exception.InvalidMemoryItemOrderException;
import com.memories.platform.memory.exception.InvalidMemoryLocationReferenceException;
import com.memories.platform.memory.exception.MemoryEventConflictException;
import com.memories.platform.memory.exception.MemoryEventNotFoundException;
import com.memories.platform.memory.exception.MemoryVersionConflictException;
import com.memories.platform.memory.repository.MemoryEventRepository;
import com.memories.platform.memory.repository.MemoryLocationRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class MemoryEventService {

    private final MemoryEventRepository eventRepository;
    private final MemoryLocationRepository locationRepository;
    private final MemoryDraftAccessService accessService;
    private final MemoryContentSafetyService contentSafetyService;
    private final MemoryScheduleValidationService validationService;
    private final Clock clock;

    public MemoryEventService(
            MemoryEventRepository eventRepository,
            MemoryLocationRepository locationRepository,
            MemoryDraftAccessService accessService,
            MemoryContentSafetyService contentSafetyService,
            MemoryScheduleValidationService validationService,
            Clock clock
    ) {
        this.eventRepository = eventRepository;
        this.locationRepository = locationRepository;
        this.accessService = accessService;
        this.contentSafetyService = contentSafetyService;
        this.validationService = validationService;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<MemoryEventResponse> list(UUID memoryId) {
        accessService.requireOwned(memoryId);
        return eventRepository.findAllByMemoryIdOrderBySortOrderAsc(memoryId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public MemoryEventResponse create(UUID memoryId, CreateMemoryEventRequest request) {
        accessService.requireEditable(memoryId);
        requireValid(memoryId, request);
        UUID actorId = accessService.actorId();
        Instant now = clock.instant();
        MemoryEvent event = new MemoryEvent(
                UUID.randomUUID(),
                memoryId,
                request.locationId(),
                request.eventType(),
                request.title(),
                request.description(),
                request.startAt(),
                request.endAt(),
                request.timezone(),
                request.sortOrder(),
                request.rsvpEnabled(),
                actorId,
                now
        );
        try {
            eventRepository.saveAndFlush(event);
        } catch (DataIntegrityViolationException exception) {
            throw new MemoryEventConflictException();
        }
        return toResponse(event);
    }

    @Transactional
    public MemoryEventResponse update(
            UUID memoryId,
            UUID eventId,
            UpdateMemoryEventRequest request
    ) {
        accessService.requireEditable(memoryId);
        MemoryEvent event = find(memoryId, eventId);
        requireVersion(event, request.version());
        requireValid(memoryId, request);
        event.update(
                request.locationId(),
                request.eventType(),
                request.title(),
                request.description(),
                request.startAt(),
                request.endAt(),
                request.timezone(),
                request.rsvpEnabled(),
                accessService.actorId(),
                clock.instant()
        );
        flushForUpdate();
        return toResponse(event);
    }

    @Transactional
    public void delete(UUID memoryId, UUID eventId, long version) {
        accessService.requireEditable(memoryId);
        MemoryEvent event = find(memoryId, eventId);
        requireVersion(event, version);
        eventRepository.delete(event);
        flushForUpdate();
    }

    @Transactional
    public List<MemoryEventResponse> reorder(
            UUID memoryId,
            ReorderMemoryItemsRequest request
    ) {
        accessService.requireEditable(memoryId);
        List<MemoryEvent> events = eventRepository.findAllByMemoryIdOrderBySortOrderAsc(memoryId);
        validateOrder(events, request);
        List<UUID> currentOrder = events.stream().map(MemoryEvent::getId).toList();
        if (currentOrder.equals(request.orderedIds())) {
            return events.stream().map(this::toResponse).toList();
        }

        int temporaryBase = temporaryBase(
                events.stream().mapToInt(MemoryEvent::getSortOrder).max().orElse(0),
                events.size()
        );
        UUID actorId = accessService.actorId();
        Instant now = clock.instant();
        Map<UUID, MemoryEvent> eventsById = events.stream().collect(
                java.util.stream.Collectors.toMap(MemoryEvent::getId, event -> event)
        );
        for (int index = 0; index < request.orderedIds().size(); index++) {
            eventsById.get(request.orderedIds().get(index))
                    .reorder(temporaryBase + index, actorId, now);
        }
        flushForUpdate();
        for (int index = 0; index < request.orderedIds().size(); index++) {
            eventsById.get(request.orderedIds().get(index)).reorder(index, actorId, now);
        }
        flushForUpdate();
        events.sort(Comparator.comparingInt(MemoryEvent::getSortOrder));
        return events.stream().map(this::toResponse).toList();
    }

    private void requireValid(UUID memoryId, CreateMemoryEventRequest request) {
        requireSafe(request.title(), request.description());
        validationService.requireValidTimeRange(request.startAt(), request.endAt());
        validationService.requireValidTimezone(request.timezone());
        requireLocation(memoryId, request.locationId());
    }

    private void requireValid(UUID memoryId, UpdateMemoryEventRequest request) {
        requireSafe(request.title(), request.description());
        validationService.requireValidTimeRange(request.startAt(), request.endAt());
        validationService.requireValidTimezone(request.timezone());
        requireLocation(memoryId, request.locationId());
    }

    private void requireLocation(UUID memoryId, UUID locationId) {
        if (locationId != null && locationRepository.findReference(locationId, memoryId).isEmpty()) {
            throw new InvalidMemoryLocationReferenceException();
        }
    }

    private void validateOrder(
            List<MemoryEvent> events,
            ReorderMemoryItemsRequest request
    ) {
        Set<UUID> entityIds = events.stream()
                .map(MemoryEvent::getId)
                .collect(java.util.stream.Collectors.toSet());
        Set<UUID> orderedIds = new HashSet<>(request.orderedIds());
        if (orderedIds.size() != request.orderedIds().size()
                || !orderedIds.equals(entityIds)
                || !request.versions().keySet().equals(entityIds)) {
            throw new InvalidMemoryItemOrderException();
        }
        events.forEach(event -> requireVersion(
                event,
                request.versions().get(event.getId())
        ));
    }

    private int temporaryBase(int maximumSortOrder, int itemCount) {
        long base = (long) maximumSortOrder + itemCount + 1;
        if (base + itemCount > Integer.MAX_VALUE) {
            throw new InvalidMemoryItemOrderException();
        }
        return (int) base;
    }

    private void requireSafe(String... values) {
        for (String value : values) {
            contentSafetyService.requireSafeMarkdown(value);
        }
    }

    private MemoryEvent find(UUID memoryId, UUID eventId) {
        return eventRepository.findByIdAndMemoryId(eventId, memoryId)
                .orElseThrow(MemoryEventNotFoundException::new);
    }

    private void requireVersion(MemoryEvent event, Long expectedVersion) {
        if (expectedVersion == null || event.getVersion() != expectedVersion) {
            throw new MemoryVersionConflictException(event.getVersion());
        }
    }

    private void flushForUpdate() {
        try {
            eventRepository.flush();
        } catch (ObjectOptimisticLockingFailureException exception) {
            throw new MemoryVersionConflictException(null);
        } catch (DataIntegrityViolationException exception) {
            throw new MemoryEventConflictException();
        }
    }

    private MemoryEventResponse toResponse(MemoryEvent event) {
        return new MemoryEventResponse(
                event.getId(),
                event.getLocationId(),
                event.getEventType(),
                event.getTitle(),
                event.getDescription(),
                event.getStartAt(),
                event.getEndAt(),
                event.getTimezone(),
                event.getSortOrder(),
                event.isRsvpEnabled(),
                event.getCreatedAt(),
                event.getUpdatedAt(),
                event.getVersion()
        );
    }
}
