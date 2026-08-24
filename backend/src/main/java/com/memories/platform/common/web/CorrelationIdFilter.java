package com.memories.platform.common.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
public class CorrelationIdFilter extends OncePerRequestFilter {

    public static final String HEADER_NAME = "X-Correlation-Id";
    public static final String MDC_KEY = "correlationId";
    public static final String REQUEST_ATTRIBUTE = CorrelationIdFilter.class.getName() + ".correlationId";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String correlationId = resolveCorrelationId(request.getHeader(HEADER_NAME));
        request.setAttribute(REQUEST_ATTRIBUTE, correlationId);
        response.setHeader(HEADER_NAME, correlationId);
        MDC.put(MDC_KEY, correlationId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_KEY);
        }
    }

    private String resolveCorrelationId(String requestedCorrelationId) {
        if (requestedCorrelationId == null || requestedCorrelationId.isBlank()) {
            return UUID.randomUUID().toString();
        }

        String trimmedCorrelationId = requestedCorrelationId.trim();
        try {
            UUID parsedCorrelationId = UUID.fromString(trimmedCorrelationId);
            if (parsedCorrelationId.toString().equalsIgnoreCase(trimmedCorrelationId)) {
                return parsedCorrelationId.toString();
            }
        } catch (IllegalArgumentException exception) {
            // Invalid client values are replaced below.
        }
        return UUID.randomUUID().toString();
    }
}
