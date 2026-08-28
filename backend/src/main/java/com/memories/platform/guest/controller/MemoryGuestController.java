package com.memories.platform.guest.controller;

import com.memories.platform.common.web.LogActivity;
import com.memories.platform.guest.dto.CreateMemoryGuestRequest;
import com.memories.platform.guest.dto.GuestAccessTokenResponse;
import com.memories.platform.guest.dto.MemoryGuestPageResponse;
import com.memories.platform.guest.dto.MemoryGuestResponse;
import com.memories.platform.guest.dto.MemoryGuestVersionRequest;
import com.memories.platform.guest.dto.UpdateMemoryGuestRequest;
import com.memories.platform.guest.service.MemoryGuestService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
@RequestMapping("/api/v1/memories/{memoryId}/guests")
public class MemoryGuestController {

    private final MemoryGuestService guestService;

    public MemoryGuestController(MemoryGuestService guestService) {
        this.guestService = guestService;
    }

    @LogActivity("List guests for a memory")
    @GetMapping
    public ResponseEntity<MemoryGuestPageResponse> list(
            @PathVariable UUID memoryId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(guestService.list(memoryId, page, size));
    }

    @LogActivity("Add a guest to a memory")
    @PostMapping
    public ResponseEntity<MemoryGuestResponse> create(
            @PathVariable UUID memoryId,
            @Valid @RequestBody CreateMemoryGuestRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                guestService.create(memoryId, request)
        );
    }

    @LogActivity("Update a memory guest")
    @PutMapping("/{guestId}")
    public ResponseEntity<MemoryGuestResponse> update(
            @PathVariable UUID memoryId,
            @PathVariable UUID guestId,
            @Valid @RequestBody UpdateMemoryGuestRequest request
    ) {
        return ResponseEntity.ok(guestService.update(memoryId, guestId, request));
    }

    @LogActivity("Issue an access token for a memory guest")
    @PostMapping("/{guestId}/access-token")
    public ResponseEntity<GuestAccessTokenResponse> issueAccessToken(
            @PathVariable UUID memoryId,
            @PathVariable UUID guestId,
            @Valid @RequestBody MemoryGuestVersionRequest request
    ) {
        return ResponseEntity.ok(
                guestService.issueAccessToken(memoryId, guestId, request)
        );
    }

    @LogActivity("Disable a memory guest")
    @PostMapping("/{guestId}/disable")
    public ResponseEntity<Void> disable(
            @PathVariable UUID memoryId,
            @PathVariable UUID guestId,
            @Valid @RequestBody MemoryGuestVersionRequest request
    ) {
        guestService.disable(memoryId, guestId, request);
        return ResponseEntity.noContent().build();
    }
}
