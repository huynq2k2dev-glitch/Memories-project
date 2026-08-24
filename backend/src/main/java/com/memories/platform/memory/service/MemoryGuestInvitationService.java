package com.memories.platform.memory.service;

import com.memories.platform.memory.dto.GuestMemoryContextResponse;
import com.memories.platform.memory.entity.Memory;
import com.memories.platform.memory.entity.MemoryStatus;
import com.memories.platform.memory.exception.MemoryNotFoundException;
import com.memories.platform.memory.repository.MemoryEventRepository;
import com.memories.platform.memory.repository.MemoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.UUID;

@Service
public class MemoryGuestInvitationService {

    private final MemoryRepository memoryRepository;
    private final MemoryEventRepository eventRepository;
    private final Clock clock;

    public MemoryGuestInvitationService(
            MemoryRepository memoryRepository,
            MemoryEventRepository eventRepository,
            Clock clock
    ) {
        this.memoryRepository = memoryRepository;
        this.eventRepository = eventRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public GuestMemoryContextResponse get(UUID memoryId) {
        Memory memory = requireAvailableMemory(memoryId);
        return new GuestMemoryContextResponse(
                memory.getTitle(),
                eventRepository
                        .findAllByMemoryIdAndRsvpEnabledTrueOrderBySortOrderAsc(memoryId)
                        .stream()
                        .map(event -> new GuestMemoryContextResponse.Event(
                                event.getId(),
                                event.getEventType(),
                                event.getTitle(),
                                event.getDescription(),
                                event.getStartAt(),
                                event.getEndAt(),
                                event.getTimezone(),
                                event.getSortOrder()
                        ))
                        .toList()
        );
    }

    @Transactional(readOnly = true)
    public void requireRsvpEvent(UUID memoryId, UUID eventId) {
        requireAvailableMemory(memoryId);
        if (eventRepository.findByIdAndMemoryId(eventId, memoryId)
                .filter(event -> event.isRsvpEnabled())
                .isEmpty()) {
            throw new MemoryNotFoundException();
        }
    }

    private Memory requireAvailableMemory(UUID memoryId) {
        return memoryRepository.findGuestInvitationMemory(
                memoryId,
                MemoryStatus.PUBLISHED,
                clock.instant()
        ).orElseThrow(MemoryNotFoundException::new);
    }
}
