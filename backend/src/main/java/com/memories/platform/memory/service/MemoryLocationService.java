package com.memories.platform.memory.service;

import com.memories.platform.memory.dto.CreateMemoryLocationRequest;
import com.memories.platform.memory.dto.MemoryLocationResponse;
import com.memories.platform.memory.dto.ReorderMemoryItemsRequest;
import com.memories.platform.memory.dto.UpdateMemoryLocationRequest;
import com.memories.platform.memory.entity.MemoryLocation;
import com.memories.platform.memory.exception.InvalidMemoryItemOrderException;
import com.memories.platform.memory.exception.MemoryLocationConflictException;
import com.memories.platform.memory.exception.MemoryLocationNotFoundException;
import com.memories.platform.memory.exception.MemoryVersionConflictException;
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
public class MemoryLocationService {

    private final MemoryLocationRepository locationRepository;
    private final MemoryAccessService accessService;
    private final MemoryContentSafetyService contentSafetyService;
    private final MemoryScheduleValidationService validationService;
    private final Clock clock;

    public MemoryLocationService(
            MemoryLocationRepository locationRepository,
            MemoryAccessService accessService,
            MemoryContentSafetyService contentSafetyService,
            MemoryScheduleValidationService validationService,
            Clock clock
    ) {
        this.locationRepository = locationRepository;
        this.accessService = accessService;
        this.contentSafetyService = contentSafetyService;
        this.validationService = validationService;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<MemoryLocationResponse> list(UUID memoryId) {
        accessService.requireView(memoryId);
        return locationRepository.findAllByMemoryIdOrderBySortOrderAsc(memoryId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public MemoryLocationResponse create(
            UUID memoryId,
            CreateMemoryLocationRequest request
    ) {
        accessService.requireEditable(memoryId);
        requireValid(request);
        UUID actorId = accessService.actorId();
        Instant now = clock.instant();
        MemoryLocation location = new MemoryLocation(
                UUID.randomUUID(),
                memoryId,
                request.name(),
                request.address(),
                request.latitude(),
                request.longitude(),
                request.mapUrl(),
                request.note(),
                request.sortOrder(),
                actorId,
                now
        );
        try {
            locationRepository.saveAndFlush(location);
        } catch (DataIntegrityViolationException exception) {
            throw new MemoryLocationConflictException();
        }
        return toResponse(location);
    }

    @Transactional
    public MemoryLocationResponse update(
            UUID memoryId,
            UUID locationId,
            UpdateMemoryLocationRequest request
    ) {
        accessService.requireEditable(memoryId);
        MemoryLocation location = find(memoryId, locationId);
        requireVersion(location, request.version());
        requireValid(request);
        location.update(
                request.name(),
                request.address(),
                request.latitude(),
                request.longitude(),
                request.mapUrl(),
                request.note(),
                accessService.actorId(),
                clock.instant()
        );
        flushForUpdate();
        return toResponse(location);
    }

    @Transactional
    public void delete(UUID memoryId, UUID locationId, long version) {
        accessService.requireEditable(memoryId);
        MemoryLocation location = find(memoryId, locationId);
        requireVersion(location, version);
        locationRepository.delete(location);
        flushForUpdate();
    }

    @Transactional
    public List<MemoryLocationResponse> reorder(
            UUID memoryId,
            ReorderMemoryItemsRequest request
    ) {
        accessService.requireEditable(memoryId);
        List<MemoryLocation> locations = locationRepository
                .findAllByMemoryIdOrderBySortOrderAsc(memoryId);
        validateOrder(locations, request);
        List<UUID> currentOrder = locations.stream().map(MemoryLocation::getId).toList();
        if (currentOrder.equals(request.orderedIds())) {
            return locations.stream().map(this::toResponse).toList();
        }

        UUID actorId = accessService.actorId();
        Instant now = clock.instant();
        Map<UUID, MemoryLocation> locationsById = locations.stream().collect(
                java.util.stream.Collectors.toMap(MemoryLocation::getId, location -> location)
        );
        for (int index = 0; index < request.orderedIds().size(); index++) {
            locationsById.get(request.orderedIds().get(index)).reorder(index, actorId, now);
        }
        flushForUpdate();
        locations.sort(Comparator.comparingInt(MemoryLocation::getSortOrder));
        return locations.stream().map(this::toResponse).toList();
    }

    private void validateOrder(
            List<MemoryLocation> locations,
            ReorderMemoryItemsRequest request
    ) {
        Set<UUID> entityIds = locations.stream()
                .map(MemoryLocation::getId)
                .collect(java.util.stream.Collectors.toSet());
        Set<UUID> orderedIds = new HashSet<>(request.orderedIds());
        if (orderedIds.size() != request.orderedIds().size()
                || !orderedIds.equals(entityIds)
                || !request.versions().keySet().equals(entityIds)) {
            throw new InvalidMemoryItemOrderException();
        }
        locations.forEach(location -> requireVersion(
                location,
                request.versions().get(location.getId())
        ));
    }

    private void requireValid(CreateMemoryLocationRequest request) {
        requireSafe(request.name(), request.address(), request.note());
        validationService.requireValidCoordinates(request.latitude(), request.longitude());
        validationService.requireSafeMapUrl(request.mapUrl());
    }

    private void requireValid(UpdateMemoryLocationRequest request) {
        requireSafe(request.name(), request.address(), request.note());
        validationService.requireValidCoordinates(request.latitude(), request.longitude());
        validationService.requireSafeMapUrl(request.mapUrl());
    }

    private void requireSafe(String... values) {
        for (String value : values) {
            contentSafetyService.requireSafeMarkdown(value);
        }
    }

    private MemoryLocation find(UUID memoryId, UUID locationId) {
        return locationRepository.findByIdAndMemoryId(locationId, memoryId)
                .orElseThrow(MemoryLocationNotFoundException::new);
    }

    private void requireVersion(MemoryLocation location, Long expectedVersion) {
        if (expectedVersion == null || location.getVersion() != expectedVersion) {
            throw new MemoryVersionConflictException(location.getVersion());
        }
    }

    private void flushForUpdate() {
        try {
            locationRepository.flush();
        } catch (ObjectOptimisticLockingFailureException exception) {
            throw new MemoryVersionConflictException(null);
        } catch (DataIntegrityViolationException exception) {
            throw new MemoryLocationConflictException();
        }
    }

    private MemoryLocationResponse toResponse(MemoryLocation location) {
        return new MemoryLocationResponse(
                location.getId(),
                location.getName(),
                location.getAddress(),
                location.getLatitude(),
                location.getLongitude(),
                location.getMapUrl(),
                location.getNote(),
                location.getSortOrder(),
                location.getCreatedAt(),
                location.getUpdatedAt(),
                location.getVersion()
        );
    }
}
