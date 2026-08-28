package com.memories.platform.guest.controller;

import com.memories.platform.common.web.LogActivity;
import com.memories.platform.guest.dto.GuestInvitationResponse;
import com.memories.platform.guest.dto.GuestRsvpResponse;
import com.memories.platform.guest.dto.SubmitGuestRsvpRequest;
import com.memories.platform.guest.service.MemoryGuestService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/public/guests")
public class PublicGuestController {

    private final MemoryGuestService guestService;

    public PublicGuestController(MemoryGuestService guestService) {
        this.guestService = guestService;
    }

    @LogActivity("Get a guest invitation by access token")
    @GetMapping("/{accessToken}")
    public ResponseEntity<GuestInvitationResponse> invitation(
            @PathVariable String accessToken
    ) {
        return ResponseEntity.ok(guestService.invitation(accessToken));
    }

    @LogActivity("Submit a guest RSVP response")
    @PostMapping("/{accessToken}/responses")
    public ResponseEntity<GuestRsvpResponse> respond(
            @PathVariable String accessToken,
            @Valid @RequestBody SubmitGuestRsvpRequest request
    ) {
        return ResponseEntity.ok(guestService.respond(accessToken, request));
    }
}
