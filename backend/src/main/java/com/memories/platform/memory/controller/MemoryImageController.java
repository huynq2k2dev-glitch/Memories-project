package com.memories.platform.memory.controller;

import com.memories.platform.common.web.LogActivity;
import com.memories.platform.memory.dto.CreateMemoryImageRequest;
import com.memories.platform.memory.dto.MemoryImageResponse;
import com.memories.platform.memory.dto.ReorderMemoryItemsRequest;
import com.memories.platform.memory.dto.UpdateMemoryImageRequest;
import com.memories.platform.memory.service.MemoryImageService;
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
@RequestMapping("/api/v1/memories/{memoryId}/images")
public class MemoryImageController {

    private final MemoryImageService imageService;

    public MemoryImageController(MemoryImageService imageService) {
        this.imageService = imageService;
    }

    @LogActivity("List images in a memory")
    @GetMapping
    public ResponseEntity<List<MemoryImageResponse>> list(@PathVariable UUID memoryId) {
        return ResponseEntity.ok(imageService.list(memoryId));
    }

    @LogActivity("Add an image to a memory")
    @PostMapping
    public ResponseEntity<MemoryImageResponse> create(
            @PathVariable UUID memoryId,
            @Valid @RequestBody CreateMemoryImageRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                imageService.create(memoryId, request)
        );
    }

    @LogActivity("Reorder memory images")
    @PutMapping("/order")
    public ResponseEntity<List<MemoryImageResponse>> reorder(
            @PathVariable UUID memoryId,
            @Valid @RequestBody ReorderMemoryItemsRequest request
    ) {
        return ResponseEntity.ok(imageService.reorder(memoryId, request));
    }

    @LogActivity("Update a memory image")
    @PutMapping("/{imageId}")
    public ResponseEntity<MemoryImageResponse> update(
            @PathVariable UUID memoryId,
            @PathVariable UUID imageId,
            @Valid @RequestBody UpdateMemoryImageRequest request
    ) {
        return ResponseEntity.ok(imageService.update(memoryId, imageId, request));
    }

    @LogActivity("Delete a memory image")
    @DeleteMapping("/{imageId}")
    public ResponseEntity<Void> delete(
            @PathVariable UUID memoryId,
            @PathVariable UUID imageId,
            @RequestParam long version
    ) {
        imageService.delete(memoryId, imageId, version);
        return ResponseEntity.noContent().build();
    }
}
