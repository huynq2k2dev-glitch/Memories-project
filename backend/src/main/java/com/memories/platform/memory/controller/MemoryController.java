package com.memories.platform.memory.controller;

import com.memories.platform.common.web.CorrelationIdFilter;
import com.memories.platform.common.web.LogActivity;
import com.memories.platform.memory.dto.ArchiveMemoryRequest;
import com.memories.platform.memory.dto.CreateMemoryRequest;
import com.memories.platform.memory.dto.MemoryCoverResponse;
import com.memories.platform.memory.dto.MemoryDetailResponse;
import com.memories.platform.memory.dto.MemoryLifecycleResponse;
import com.memories.platform.memory.dto.MemoryPageResponse;
import com.memories.platform.memory.dto.MemoryRenderResponse;
import com.memories.platform.memory.dto.PublishMemoryRequest;
import com.memories.platform.memory.dto.PublishMemoryResponse;
import com.memories.platform.memory.dto.UpdateMemoryAssetReferenceRequest;
import com.memories.platform.memory.dto.UpdateMemoryRequest;
import com.memories.platform.memory.service.MemoryLifecycleService;
import com.memories.platform.memory.service.MemoryPublishingService;
import com.memories.platform.memory.service.MemoryRenderService;
import com.memories.platform.memory.service.MemoryService;
import jakarta.servlet.http.HttpServletRequest;
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

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/memories")
public class MemoryController {

    private final MemoryService memoryService;
    private final MemoryPublishingService publishingService;
    private final MemoryLifecycleService lifecycleService;
    private final MemoryRenderService renderService;

    public MemoryController(
            MemoryService memoryService,
            MemoryPublishingService publishingService,
            MemoryLifecycleService lifecycleService,
            MemoryRenderService renderService
    ) {
        this.memoryService = memoryService;
        this.publishingService = publishingService;
        this.lifecycleService = lifecycleService;
        this.renderService = renderService;
    }

    @LogActivity("Create a memory")
    @PostMapping
    public ResponseEntity<MemoryDetailResponse> create(
            @Valid @RequestBody CreateMemoryRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(memoryService.create(request));
    }

    @LogActivity("List memories owned by the current account")
    @GetMapping
    public ResponseEntity<MemoryPageResponse> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size
    ) {
        return ResponseEntity.ok(memoryService.listOwned(page, size));
    }

    @LogActivity("Get memory details")
    @GetMapping("/{memoryId}")
    public ResponseEntity<MemoryDetailResponse> get(@PathVariable UUID memoryId) {
        return ResponseEntity.ok(memoryService.get(memoryId));
    }

    @LogActivity("Update memory details")
    @PutMapping("/{memoryId}")
    public ResponseEntity<MemoryDetailResponse> update(
            @PathVariable UUID memoryId,
            @Valid @RequestBody UpdateMemoryRequest request
    ) {
        return ResponseEntity.ok(memoryService.update(memoryId, request));
    }

    @LogActivity("Update a memory cover")
    @PutMapping("/{memoryId}/cover")
    public ResponseEntity<MemoryCoverResponse> updateCover(
            @PathVariable UUID memoryId,
            @Valid @RequestBody UpdateMemoryAssetReferenceRequest request
    ) {
        return ResponseEntity.ok(memoryService.updateCover(memoryId, request));
    }

    @LogActivity("Preview a memory")
    @GetMapping("/{memoryId}/preview")
    public ResponseEntity<MemoryRenderResponse> preview(@PathVariable UUID memoryId) {
        return ResponseEntity.ok(renderService.preview(memoryId));
    }

    @LogActivity("Publish a memory")
    @PostMapping("/{memoryId}/publish")
    public ResponseEntity<PublishMemoryResponse> publish(
            @PathVariable UUID memoryId,
            @Valid @RequestBody PublishMemoryRequest request,
            HttpServletRequest httpRequest
    ) {
        String correlationId = (String) httpRequest.getAttribute(
                CorrelationIdFilter.REQUEST_ATTRIBUTE
        );
        return ResponseEntity.ok(publishingService.publish(memoryId, request, correlationId));
    }

    @LogActivity("Archive a memory")
    @PostMapping("/{memoryId}/archive")
    public ResponseEntity<MemoryLifecycleResponse> archive(
            @PathVariable UUID memoryId,
            @Valid @RequestBody ArchiveMemoryRequest request,
            HttpServletRequest httpRequest
    ) {
        String correlationId = (String) httpRequest.getAttribute(
                CorrelationIdFilter.REQUEST_ATTRIBUTE
        );
        return ResponseEntity.ok(lifecycleService.archive(memoryId, request, correlationId));
    }

    @LogActivity("Soft-delete a memory")
    @DeleteMapping("/{memoryId}")
    public ResponseEntity<Void> delete(
            @PathVariable UUID memoryId,
            @RequestParam long version,
            HttpServletRequest httpRequest
    ) {
        String correlationId = (String) httpRequest.getAttribute(
                CorrelationIdFilter.REQUEST_ATTRIBUTE
        );
        lifecycleService.softDelete(memoryId, version, correlationId);
        return ResponseEntity.noContent().build();
    }
}
