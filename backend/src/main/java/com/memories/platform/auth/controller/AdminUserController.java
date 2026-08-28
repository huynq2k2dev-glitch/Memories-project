package com.memories.platform.auth.controller;

import com.memories.platform.auth.dto.AccountStatusResponse;
import com.memories.platform.auth.service.UserAccountAdministrationService;
import com.memories.platform.common.web.CorrelationIdFilter;
import com.memories.platform.common.web.LogActivity;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/users")
public class AdminUserController {

    private final UserAccountAdministrationService administrationService;

    public AdminUserController(UserAccountAdministrationService administrationService) {
        this.administrationService = administrationService;
    }

    @LogActivity("Lock a user account")
    @PostMapping("/{userId}/lock")
    public ResponseEntity<AccountStatusResponse> lock(
            @PathVariable UUID userId,
            HttpServletRequest request
    ) {
        String correlationId = (String) request.getAttribute(CorrelationIdFilter.REQUEST_ATTRIBUTE);
        return ResponseEntity.ok(administrationService.lock(userId, correlationId));
    }
}
