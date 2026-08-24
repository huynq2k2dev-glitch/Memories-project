package com.memories.platform.guest.controller;

import com.memories.platform.common.web.CorrelationIdFilter;
import com.memories.platform.guest.dto.GuestMessageModerationResponse;
import com.memories.platform.guest.dto.GuestMessagePageResponse;
import com.memories.platform.guest.dto.ModerateGuestMessageRequest;
import com.memories.platform.guest.entity.GuestMessageStatus;
import com.memories.platform.guest.service.GuestMessageService;
import com.memories.platform.memory.dto.MessageModerationSettingsResponse;
import com.memories.platform.memory.dto.UpdateMessageModerationRequest;
import com.memories.platform.memory.service.MemoryMessageSettingsService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/memories/{memoryId}/messages")
public class MemoryGuestMessageController {

    private final GuestMessageService messageService;
    private final MemoryMessageSettingsService settingsService;

    public MemoryGuestMessageController(
            GuestMessageService messageService,
            MemoryMessageSettingsService settingsService
    ) {
        this.messageService = messageService;
        this.settingsService = settingsService;
    }

    @GetMapping
    public ResponseEntity<GuestMessagePageResponse> list(
            @PathVariable UUID memoryId,
            @RequestParam(required = false) GuestMessageStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(messageService.list(memoryId, status, page, size));
    }

    @PutMapping("/{messageId}")
    public ResponseEntity<GuestMessageModerationResponse> moderate(
            @PathVariable UUID memoryId,
            @PathVariable UUID messageId,
            @Valid @RequestBody ModerateGuestMessageRequest request,
            HttpServletRequest httpRequest
    ) {
        return ResponseEntity.ok(messageService.moderate(
                memoryId,
                messageId,
                request,
                correlationId(httpRequest)
        ));
    }

    @PutMapping("/settings")
    public ResponseEntity<MessageModerationSettingsResponse> updateSettings(
            @PathVariable UUID memoryId,
            @Valid @RequestBody UpdateMessageModerationRequest request,
            HttpServletRequest httpRequest
    ) {
        return ResponseEntity.ok(settingsService.update(
                memoryId,
                request,
                correlationId(httpRequest)
        ));
    }

    private String correlationId(HttpServletRequest request) {
        return (String) request.getAttribute(CorrelationIdFilter.REQUEST_ATTRIBUTE);
    }
}
