package com.memories.platform.memory.controller;

import com.memories.platform.media.dto.MediaDeliveryResponse;
import com.memories.platform.memory.dto.MemoryRenderResponse;
import com.memories.platform.memory.service.MemoryRenderService;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/public")
public class PublicMemoryController {

    private final MemoryRenderService renderService;

    public PublicMemoryController(MemoryRenderService renderService) {
        this.renderService = renderService;
    }

    @GetMapping("/memories/{slug}")
    public ResponseEntity<MemoryRenderResponse> memory(@PathVariable String slug) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(renderService.publicMemory(slug));
    }

    @GetMapping("/media/{assetId}")
    public ResponseEntity<Void> media(@PathVariable UUID assetId) {
        MediaDeliveryResponse delivery = renderService.publicDelivery(assetId);
        return ResponseEntity.status(302)
                .location(URI.create(delivery.url()))
                .cacheControl(CacheControl.noStore())
                .build();
    }
}
