package com.memories.platform.template.service;

import com.memories.platform.auth.service.AuthorizationService;
import com.memories.platform.template.constants.TemplateConstants;
import com.memories.platform.template.dto.AdminTemplateResponse;
import com.memories.platform.template.dto.AdminTemplateVersionResponse;
import com.memories.platform.template.dto.CreateTemplateRequest;
import com.memories.platform.template.dto.UpdateTemplateRequest;
import com.memories.platform.template.dto.UpsertTemplateVersionRequest;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class TemplateAdministrationService {

    private final AuthorizationService authorizationService;
    private final TemplateAdministrationPersistenceService persistenceService;

    public TemplateAdministrationService(
            AuthorizationService authorizationService,
            TemplateAdministrationPersistenceService persistenceService
    ) {
        this.authorizationService = authorizationService;
        this.persistenceService = persistenceService;
    }

    public List<AdminTemplateResponse> list(String correlationId) {
        authorize(correlationId);
        return persistenceService.list();
    }

    public AdminTemplateResponse create(CreateTemplateRequest request, String correlationId) {
        authorize(correlationId);
        return persistenceService.create(request);
    }

    public AdminTemplateResponse update(
            UUID templateId,
            UpdateTemplateRequest request,
            String correlationId
    ) {
        authorize(correlationId);
        return persistenceService.update(templateId, request);
    }

    public AdminTemplateVersionResponse createVersion(
            UUID templateId,
            UpsertTemplateVersionRequest request,
            String correlationId
    ) {
        authorize(correlationId);
        return persistenceService.createVersion(templateId, request);
    }

    public AdminTemplateVersionResponse updateVersion(
            UUID templateId,
            UUID versionId,
            UpsertTemplateVersionRequest request,
            String correlationId
    ) {
        authorize(correlationId);
        return persistenceService.updateVersion(templateId, versionId, request);
    }

    public AdminTemplateVersionResponse publish(
            UUID templateId,
            UUID versionId,
            String correlationId
    ) {
        authorize(correlationId);
        return persistenceService.publish(templateId, versionId);
    }

    public AdminTemplateVersionResponse deprecate(
            UUID templateId,
            UUID versionId,
            String correlationId
    ) {
        authorize(correlationId);
        return persistenceService.deprecate(templateId, versionId);
    }

    private void authorize(String correlationId) {
        authorizationService.requirePermission(
                TemplateConstants.PERMISSION_TEMPLATE_MANAGE,
                correlationId,
                TemplateConstants.AUDIT_TEMPLATE_MANAGE,
                null
        );
    }
}
