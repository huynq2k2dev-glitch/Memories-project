package com.memories.platform.health.controller;

import com.memories.platform.common.web.LogActivity;
import com.memories.platform.health.dto.HealthResponse;
import com.memories.platform.health.service.HealthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/health")
public class HealthController {

    private final HealthService healthService;

    public HealthController(HealthService healthService) {
        this.healthService = healthService;
    }

    @LogActivity("Get the platform health status")
    @GetMapping
    public ResponseEntity<HealthResponse> getHealth() {
        return ResponseEntity.ok(healthService.getHealth());
    }
}
