package com.memories.platform.memory.controller;

import com.memories.platform.memory.dto.CreateMemoryEventRequest;
import com.memories.platform.memory.dto.MemoryEventResponse;
import com.memories.platform.memory.dto.ReorderMemoryItemsRequest;
import com.memories.platform.memory.dto.UpdateMemoryEventRequest;
import com.memories.platform.memory.service.MemoryEventService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/memories/{memoryId}/events")
public class MemoryEventController {

    private final MemoryEventService eventService;

    public MemoryEventController(MemoryEventService eventService) {
        this.eventService = eventService;
    }

    @GetMapping
    public ResponseEntity<List<MemoryEventResponse>> list(@PathVariable UUID memoryId) {
        return ResponseEntity.ok(eventService.list(memoryId));
    }

    @PostMapping
    public ResponseEntity<MemoryEventResponse> create(
            @PathVariable UUID memoryId,
            @Valid @RequestBody CreateMemoryEventRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                eventService.create(memoryId, request)
        );
    }

    @PutMapping("/order")
    public ResponseEntity<List<MemoryEventResponse>> reorder(
            @PathVariable UUID memoryId,
            @Valid @RequestBody ReorderMemoryItemsRequest request
    ) {
        return ResponseEntity.ok(eventService.reorder(memoryId, request));
    }

    @PutMapping("/{eventId}")
    public ResponseEntity<MemoryEventResponse> update(
            @PathVariable UUID memoryId,
            @PathVariable UUID eventId,
            @Valid @RequestBody UpdateMemoryEventRequest request
    ) {
        return ResponseEntity.ok(eventService.update(memoryId, eventId, request));
    }

    @DeleteMapping("/{eventId}")
    public ResponseEntity<Void> delete(
            @PathVariable UUID memoryId,
            @PathVariable UUID eventId,
            @RequestParam long version
    ) {
        eventService.delete(memoryId, eventId, version);
        return ResponseEntity.noContent().build();
    }
}
