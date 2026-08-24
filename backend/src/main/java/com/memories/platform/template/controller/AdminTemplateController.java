package com.memories.platform.template.controller;

import com.memories.platform.common.web.CorrelationIdFilter;
import com.memories.platform.template.dto.AdminTemplateResponse;
import com.memories.platform.template.dto.AdminTemplatePageResponse;
import com.memories.platform.template.dto.AdminTemplateVersionResponse;
import com.memories.platform.template.dto.CreateTemplateRequest;
import com.memories.platform.template.dto.UpdateTemplateRequest;
import com.memories.platform.template.dto.UpsertTemplateVersionRequest;
import com.memories.platform.template.service.TemplateAdministrationService;
import jakarta.servlet.http.HttpServletRequest;
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
@RequestMapping("/api/v1/admin/templates")
public class AdminTemplateController {

    private final TemplateAdministrationService administrationService;

    public AdminTemplateController(TemplateAdministrationService administrationService) {
        this.administrationService = administrationService;
    }

    @GetMapping
    public ResponseEntity<AdminTemplatePageResponse> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest request
    ) {
        return ResponseEntity.ok(administrationService.list(page, size, correlationId(request)));
    }

    @PostMapping
    public ResponseEntity<AdminTemplateResponse> create(
            @Valid @RequestBody CreateTemplateRequest body,
            HttpServletRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(administrationService.create(body, correlationId(request)));
    }

    @PutMapping("/{templateId}")
    public ResponseEntity<AdminTemplateResponse> update(
            @PathVariable UUID templateId,
            @Valid @RequestBody UpdateTemplateRequest body,
            HttpServletRequest request
    ) {
        return ResponseEntity.ok(administrationService.update(templateId, body, correlationId(request)));
    }

    @PostMapping("/{templateId}/versions")
    public ResponseEntity<AdminTemplateVersionResponse> createVersion(
            @PathVariable UUID templateId,
            @Valid @RequestBody UpsertTemplateVersionRequest body,
            HttpServletRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(administrationService.createVersion(templateId, body, correlationId(request)));
    }

    @PutMapping("/{templateId}/versions/{versionId}")
    public ResponseEntity<AdminTemplateVersionResponse> updateVersion(
            @PathVariable UUID templateId,
            @PathVariable UUID versionId,
            @Valid @RequestBody UpsertTemplateVersionRequest body,
            HttpServletRequest request
    ) {
        return ResponseEntity.ok(administrationService.updateVersion(
                templateId,
                versionId,
                body,
                correlationId(request)
        ));
    }

    @PostMapping("/{templateId}/versions/{versionId}/publish")
    public ResponseEntity<AdminTemplateVersionResponse> publish(
            @PathVariable UUID templateId,
            @PathVariable UUID versionId,
            HttpServletRequest request
    ) {
        return ResponseEntity.ok(administrationService.publish(
                templateId,
                versionId,
                correlationId(request)
        ));
    }

    @PostMapping("/{templateId}/versions/{versionId}/deprecate")
    public ResponseEntity<AdminTemplateVersionResponse> deprecate(
            @PathVariable UUID templateId,
            @PathVariable UUID versionId,
            HttpServletRequest request
    ) {
        return ResponseEntity.ok(administrationService.deprecate(
                templateId,
                versionId,
                correlationId(request)
        ));
    }

    private String correlationId(HttpServletRequest request) {
        return (String) request.getAttribute(CorrelationIdFilter.REQUEST_ATTRIBUTE);
    }
}
