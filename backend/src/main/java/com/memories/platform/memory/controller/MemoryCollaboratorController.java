package com.memories.platform.memory.controller;

import com.memories.platform.common.web.CorrelationIdFilter;
import com.memories.platform.common.web.LogActivity;
import com.memories.platform.memory.dto.AddMemoryCollaboratorRequest;
import com.memories.platform.memory.dto.MemoryCollaboratorResponse;
import com.memories.platform.memory.dto.UpdateMemoryCollaboratorRequest;
import com.memories.platform.memory.service.MemoryCollaborationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/memories/{memoryId}/collaborators")
public class MemoryCollaboratorController {

    private final MemoryCollaborationService collaborationService;

    public MemoryCollaboratorController(MemoryCollaborationService collaborationService) {
        this.collaborationService = collaborationService;
    }

    @LogActivity("List collaborators for a memory")
    @GetMapping
    public ResponseEntity<List<MemoryCollaboratorResponse>> list(
            @PathVariable UUID memoryId
    ) {
        return ResponseEntity.ok(collaborationService.list(memoryId));
    }

    @LogActivity("Add a collaborator to a memory")
    @PostMapping
    public ResponseEntity<MemoryCollaboratorResponse> add(
            @PathVariable UUID memoryId,
            @Valid @RequestBody AddMemoryCollaboratorRequest request,
            HttpServletRequest httpRequest
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(collaborationService.add(
                memoryId,
                request,
                correlationId(httpRequest)
        ));
    }

    @LogActivity("Change a memory collaborator's permission")
    @PutMapping("/{collaboratorId}")
    public ResponseEntity<MemoryCollaboratorResponse> changePermission(
            @PathVariable UUID memoryId,
            @PathVariable UUID collaboratorId,
            @Valid @RequestBody UpdateMemoryCollaboratorRequest request,
            HttpServletRequest httpRequest
    ) {
        return ResponseEntity.ok(collaborationService.changePermission(
                memoryId,
                collaboratorId,
                request,
                correlationId(httpRequest)
        ));
    }

    @LogActivity("Revoke a memory collaborator")
    @DeleteMapping("/{collaboratorId}")
    public ResponseEntity<Void> revoke(
            @PathVariable UUID memoryId,
            @PathVariable UUID collaboratorId,
            HttpServletRequest httpRequest
    ) {
        collaborationService.revoke(
                memoryId,
                collaboratorId,
                correlationId(httpRequest)
        );
        return ResponseEntity.noContent().build();
    }

    private String correlationId(HttpServletRequest request) {
        return (String) request.getAttribute(CorrelationIdFilter.REQUEST_ATTRIBUTE);
    }
}
