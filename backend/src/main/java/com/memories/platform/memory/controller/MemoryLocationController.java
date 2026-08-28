package com.memories.platform.memory.controller;

import com.memories.platform.common.web.LogActivity;
import com.memories.platform.memory.dto.CreateMemoryLocationRequest;
import com.memories.platform.memory.dto.MemoryLocationResponse;
import com.memories.platform.memory.dto.ReorderMemoryItemsRequest;
import com.memories.platform.memory.dto.UpdateMemoryLocationRequest;
import com.memories.platform.memory.service.MemoryLocationService;
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
@RequestMapping("/api/v1/memories/{memoryId}/locations")
public class MemoryLocationController {

    private final MemoryLocationService locationService;

    public MemoryLocationController(MemoryLocationService locationService) {
        this.locationService = locationService;
    }

    @LogActivity("List locations in a memory")
    @GetMapping
    public ResponseEntity<List<MemoryLocationResponse>> list(@PathVariable UUID memoryId) {
        return ResponseEntity.ok(locationService.list(memoryId));
    }

    @LogActivity("Create a memory location")
    @PostMapping
    public ResponseEntity<MemoryLocationResponse> create(
            @PathVariable UUID memoryId,
            @Valid @RequestBody CreateMemoryLocationRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                locationService.create(memoryId, request)
        );
    }

    @LogActivity("Reorder memory locations")
    @PutMapping("/order")
    public ResponseEntity<List<MemoryLocationResponse>> reorder(
            @PathVariable UUID memoryId,
            @Valid @RequestBody ReorderMemoryItemsRequest request
    ) {
        return ResponseEntity.ok(locationService.reorder(memoryId, request));
    }

    @LogActivity("Update a memory location")
    @PutMapping("/{locationId}")
    public ResponseEntity<MemoryLocationResponse> update(
            @PathVariable UUID memoryId,
            @PathVariable UUID locationId,
            @Valid @RequestBody UpdateMemoryLocationRequest request
    ) {
        return ResponseEntity.ok(locationService.update(memoryId, locationId, request));
    }

    @LogActivity("Delete a memory location")
    @DeleteMapping("/{locationId}")
    public ResponseEntity<Void> delete(
            @PathVariable UUID memoryId,
            @PathVariable UUID locationId,
            @RequestParam long version
    ) {
        locationService.delete(memoryId, locationId, version);
        return ResponseEntity.noContent().build();
    }
}
