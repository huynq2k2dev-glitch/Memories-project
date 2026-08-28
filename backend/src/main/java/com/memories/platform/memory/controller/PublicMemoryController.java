package com.memories.platform.memory.controller;

import com.memories.platform.common.web.LogActivity;
import com.memories.platform.media.dto.MediaDeliveryResponse;
import com.memories.platform.memory.dto.MemoryRenderResponse;
import com.memories.platform.memory.dto.UnlockMemoryRequest;
import com.memories.platform.memory.service.MemoryPasswordAccessService;
import com.memories.platform.memory.service.MemoryRenderService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/public")
public class PublicMemoryController {

    private final MemoryRenderService renderService;
    private final MemoryPasswordAccessService passwordAccessService;

    public PublicMemoryController(
            MemoryRenderService renderService,
            MemoryPasswordAccessService passwordAccessService
    ) {
        this.renderService = renderService;
        this.passwordAccessService = passwordAccessService;
    }

    @LogActivity("Render a publicly accessible memory")
    @GetMapping("/memories/{slug}")
    public ResponseEntity<MemoryRenderResponse> memory(
            @PathVariable String slug,
            HttpServletRequest request
    ) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(renderService.publicMemory(slug, cookies(request)));
    }

    @LogActivity("Unlock a password-protected memory")
    @PostMapping("/memories/{slug}/unlock")
    public ResponseEntity<Void> unlock(
            @PathVariable String slug,
            @Valid @RequestBody UnlockMemoryRequest request
    ) {
        String accessCookie = passwordAccessService.unlock(slug, request.password());
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, accessCookie)
                .cacheControl(CacheControl.noStore())
                .build();
    }

    @LogActivity("Deliver media for a publicly accessible memory")
    @GetMapping("/media/{assetId}")
    public ResponseEntity<Void> media(
            @PathVariable UUID assetId,
            HttpServletRequest request
    ) {
        MediaDeliveryResponse delivery = renderService.publicDelivery(assetId, cookies(request));
        return ResponseEntity.status(302)
                .location(URI.create(delivery.url()))
                .cacheControl(CacheControl.noStore())
                .build();
    }

    private Map<String, String> cookies(HttpServletRequest request) {
        Cookie[] requestCookies = request.getCookies();
        if (requestCookies == null || requestCookies.length == 0) {
            return Map.of();
        }
        Map<String, String> values = new HashMap<>();
        for (Cookie cookie : requestCookies) {
            values.put(cookie.getName(), cookie.getValue());
        }
        return Map.copyOf(values);
    }
}
