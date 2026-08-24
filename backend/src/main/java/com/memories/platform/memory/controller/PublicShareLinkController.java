package com.memories.platform.memory.controller;

import com.memories.platform.guest.dto.GuestInvitationResponse;
import com.memories.platform.guest.dto.GuestRsvpResponse;
import com.memories.platform.guest.dto.SubmitGuestRsvpRequest;
import com.memories.platform.memory.constants.ShareLinkConstants;
import com.memories.platform.memory.dto.RedeemShareLinkResponse;
import com.memories.platform.memory.dto.ShareLinkRedemptionResult;
import com.memories.platform.memory.service.MemoryShareLinkService;
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

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/public")
public class PublicShareLinkController {

    private final MemoryShareLinkService shareLinkService;

    public PublicShareLinkController(MemoryShareLinkService shareLinkService) {
        this.shareLinkService = shareLinkService;
    }

    @PostMapping("/shares/{accessToken}/redeem")
    public ResponseEntity<RedeemShareLinkResponse> redeem(
            @PathVariable String accessToken
    ) {
        ShareLinkRedemptionResult redemption = shareLinkService.redeem(accessToken);
        return ResponseEntity.ok()
                .header(
                        HttpHeaders.SET_COOKIE,
                        shareLinkService.accessCookie(redemption, accessToken)
                )
                .cacheControl(CacheControl.noStore())
                .body(new RedeemShareLinkResponse(
                        ShareLinkConstants.MEMORY_PATH_PREFIX + redemption.slug(),
                        redemption.permission()
                ));
    }

    @GetMapping("/memories/{slug}/share-rsvp")
    public ResponseEntity<GuestInvitationResponse> rsvpInvitation(
            @PathVariable String slug,
            HttpServletRequest request
    ) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(shareLinkService.rsvpInvitation(slug, cookies(request)));
    }

    @PostMapping("/memories/{slug}/share-rsvp/responses")
    public ResponseEntity<GuestRsvpResponse> respond(
            @PathVariable String slug,
            @Valid @RequestBody SubmitGuestRsvpRequest request,
            HttpServletRequest httpRequest
    ) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(shareLinkService.respond(slug, cookies(httpRequest), request));
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
