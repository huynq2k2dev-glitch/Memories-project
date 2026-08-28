package com.memories.platform.media.controller;

import com.memories.platform.common.web.LogActivity;
import com.memories.platform.media.dto.MediaAssetResponse;
import com.memories.platform.media.service.MediaAssetService;
import com.memories.platform.media.service.MediaUploadService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/media")
public class MediaController {

    private final MediaUploadService uploadService;
    private final MediaAssetService assetService;

    public MediaController(MediaUploadService uploadService, MediaAssetService assetService) {
        this.uploadService = uploadService;
        this.assetService = assetService;
    }

    @LogActivity("Complete and verify a media upload")
    @PostMapping("/{assetId}/complete")
    public ResponseEntity<MediaAssetResponse> complete(@PathVariable UUID assetId) {
        return ResponseEntity.ok(uploadService.complete(assetId));
    }

    @LogActivity("Soft-delete a media asset")
    @DeleteMapping("/{assetId}")
    public ResponseEntity<Void> delete(
            @PathVariable UUID assetId,
            @RequestParam long version
    ) {
        assetService.softDelete(assetId, version);
        return ResponseEntity.noContent().build();
    }
}
