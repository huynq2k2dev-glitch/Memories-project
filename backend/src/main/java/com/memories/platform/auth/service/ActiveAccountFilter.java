package com.memories.platform.auth.service;

import com.memories.platform.common.web.CorrelationIdFilter;
import com.memories.platform.common.web.SecurityProblemWriter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;

public class ActiveAccountFilter extends OncePerRequestFilter {

    private static final Set<String> PUBLIC_PATHS = Set.of(
            "/api/v1/health",
            "/api/v1/auth/register",
            "/api/v1/auth/login",
            "/api/v1/auth/refresh",
            "/api/v1/auth/logout",
            "/api/v1/auth/email-verifications/confirm",
            "/api/v1/auth/email-verifications/resend",
            "/error"
    );

    private final ActiveAccountService activeAccountService;
    private final SecurityProblemWriter problemWriter;

    public ActiveAccountFilter(
            ActiveAccountService activeAccountService,
            SecurityProblemWriter problemWriter
    ) {
        this.activeAccountService = activeAccountService;
        this.problemWriter = problemWriter;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return PUBLIC_PATHS.contains(request.getRequestURI());
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof JwtAuthenticationToken)) {
            filterChain.doFilter(request, response);
            return;
        }

        UUID userId;
        try {
            userId = UUID.fromString(authentication.getName());
        } catch (IllegalArgumentException exception) {
            deny(request, response);
            return;
        }

        String correlationId = (String) request.getAttribute(CorrelationIdFilter.REQUEST_ATTRIBUTE);
        if (!activeAccountService.isActive(userId, correlationId)) {
            deny(request, response);
            return;
        }
        filterChain.doFilter(request, response);
    }

    private void deny(HttpServletRequest request, HttpServletResponse response) throws IOException {
        SecurityContextHolder.clearContext();
        problemWriter.write(
                request,
                response,
                HttpStatus.UNAUTHORIZED,
                "ACCOUNT_INACTIVE",
                "The account is not active."
        );
    }
}
