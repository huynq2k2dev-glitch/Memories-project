package com.memories.platform.memory.controller;

import com.memories.platform.memory.dto.CreateMemorySectionRequest;
import com.memories.platform.memory.dto.MemorySectionResponse;
import com.memories.platform.memory.dto.ReorderMemoryItemsRequest;
import com.memories.platform.memory.dto.UpdateMemorySectionRequest;
import com.memories.platform.memory.service.MemorySectionService;
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
@RequestMapping("/api/v1/memories/{memoryId}/sections")
public class MemorySectionController {

    private final MemorySectionService sectionService;

    public MemorySectionController(MemorySectionService sectionService) {
        this.sectionService = sectionService;
    }

    @GetMapping
    public ResponseEntity<List<MemorySectionResponse>> list(@PathVariable UUID memoryId) {
        return ResponseEntity.ok(sectionService.list(memoryId));
    }

    @PostMapping
    public ResponseEntity<MemorySectionResponse> create(
            @PathVariable UUID memoryId,
            @Valid @RequestBody CreateMemorySectionRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                sectionService.create(memoryId, request)
        );
    }

    @PutMapping("/order")
    public ResponseEntity<List<MemorySectionResponse>> reorder(
            @PathVariable UUID memoryId,
            @Valid @RequestBody ReorderMemoryItemsRequest request
    ) {
        return ResponseEntity.ok(sectionService.reorder(memoryId, request));
    }

    @PutMapping("/{sectionId}")
    public ResponseEntity<MemorySectionResponse> update(
            @PathVariable UUID memoryId,
            @PathVariable UUID sectionId,
            @Valid @RequestBody UpdateMemorySectionRequest request
    ) {
        return ResponseEntity.ok(sectionService.update(memoryId, sectionId, request));
    }

    @DeleteMapping("/{sectionId}")
    public ResponseEntity<Void> delete(
            @PathVariable UUID memoryId,
            @PathVariable UUID sectionId,
            @RequestParam long version
    ) {
        sectionService.delete(memoryId, sectionId, version);
        return ResponseEntity.noContent().build();
    }
}
