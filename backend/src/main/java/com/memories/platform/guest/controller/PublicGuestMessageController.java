package com.memories.platform.guest.controller;

import com.memories.platform.common.web.CorrelationIdFilter;
import com.memories.platform.common.web.LogActivity;
import com.memories.platform.guest.dto.CreateGuestMessageRequest;
import com.memories.platform.guest.service.GuestMessageService;
import com.memories.platform.ratelimit.service.ClientIpHashService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/public/memories")
public class PublicGuestMessageController {

    private final GuestMessageService messageService;
    private final ClientIpHashService ipHashService;

    public PublicGuestMessageController(
            GuestMessageService messageService,
            ClientIpHashService ipHashService
    ) {
        this.messageService = messageService;
        this.ipHashService = ipHashService;
    }

    @LogActivity("Submit a guest message to a public memory")
    @PostMapping("/{slug}/messages")
    public ResponseEntity<Void> submit(
            @PathVariable String slug,
            @Valid @RequestBody CreateGuestMessageRequest request,
            HttpServletRequest httpRequest
    ) {
        messageService.submit(
                slug,
                request,
                cookies(httpRequest),
                ipHashService.hash(httpRequest),
                (String) httpRequest.getAttribute(CorrelationIdFilter.REQUEST_ATTRIBUTE)
        );
        return ResponseEntity.accepted().build();
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
