package com.memories.platform.memory.service;

import com.memories.platform.media.dto.InitiateMediaUploadRequest;
import com.memories.platform.media.dto.InitiateMediaUploadResponse;
import com.memories.platform.media.service.MediaUploadService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class MemoryMediaUploadService {

    private final MemoryAccessService accessService;
    private final MediaUploadService uploadService;

    public MemoryMediaUploadService(
            MemoryAccessService accessService,
            MediaUploadService uploadService
    ) {
        this.accessService = accessService;
        this.uploadService = uploadService;
    }

    @Transactional
    public InitiateMediaUploadResponse initiate(
            UUID memoryId,
            InitiateMediaUploadRequest request
    ) {
        accessService.requireEditable(memoryId);
        return uploadService.initiate(request);
    }
}
