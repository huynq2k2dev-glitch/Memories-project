package com.memories.platform.memory.controller;

import com.memories.platform.common.web.CorrelationIdFilter;
import com.memories.platform.guest.dto.ShareLinkGuestResponse;
import com.memories.platform.memory.dto.CreateShareLinkRequest;
import com.memories.platform.memory.dto.IssuedShareLinkResponse;
import com.memories.platform.memory.dto.ShareLinkResponse;
import com.memories.platform.memory.service.MemoryShareLinkService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/memories/{memoryId}/share-links")
public class MemoryShareLinkController {

    private final MemoryShareLinkService shareLinkService;

    public MemoryShareLinkController(MemoryShareLinkService shareLinkService) {
        this.shareLinkService = shareLinkService;
    }

    @GetMapping
    public ResponseEntity<List<ShareLinkResponse>> list(@PathVariable UUID memoryId) {
        return ResponseEntity.ok(shareLinkService.list(memoryId));
    }

    @GetMapping("/guests")
    public ResponseEntity<List<ShareLinkGuestResponse>> eligibleGuests(
            @PathVariable UUID memoryId
    ) {
        return ResponseEntity.ok(shareLinkService.eligibleGuests(memoryId));
    }

    @PostMapping
    public ResponseEntity<IssuedShareLinkResponse> issue(
            @PathVariable UUID memoryId,
            @Valid @RequestBody CreateShareLinkRequest request,
            HttpServletRequest httpRequest
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(shareLinkService.issue(
                memoryId,
                request,
                correlationId(httpRequest)
        ));
    }

    @PostMapping("/{shareLinkId}/revoke")
    public ResponseEntity<Void> revoke(
            @PathVariable UUID memoryId,
            @PathVariable UUID shareLinkId,
            HttpServletRequest httpRequest
    ) {
        shareLinkService.revoke(memoryId, shareLinkId, correlationId(httpRequest));
        return ResponseEntity.noContent().build();
    }

    private String correlationId(HttpServletRequest request) {
        return (String) request.getAttribute(CorrelationIdFilter.REQUEST_ATTRIBUTE);
    }
}
