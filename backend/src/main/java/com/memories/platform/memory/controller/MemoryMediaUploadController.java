package com.memories.platform.memory.controller;

import com.memories.platform.media.dto.InitiateMediaUploadRequest;
import com.memories.platform.media.dto.InitiateMediaUploadResponse;
import com.memories.platform.memory.service.MemoryMediaUploadService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/memories/{memoryId}/media/uploads")
public class MemoryMediaUploadController {

    private final MemoryMediaUploadService uploadService;

    public MemoryMediaUploadController(MemoryMediaUploadService uploadService) {
        this.uploadService = uploadService;
    }

    @PostMapping
    public ResponseEntity<InitiateMediaUploadResponse> initiate(
            @PathVariable UUID memoryId,
            @Valid @RequestBody InitiateMediaUploadRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                uploadService.initiate(memoryId, request)
        );
    }
}
