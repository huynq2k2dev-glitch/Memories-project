package com.memories.platform.template.controller;

import com.memories.platform.template.dto.TemplateCatalogPageResponse;
import com.memories.platform.common.domain.MemoryType;
import com.memories.platform.common.web.LogActivity;
import com.memories.platform.template.entity.TemplateStatus;
import com.memories.platform.template.service.TemplateCatalogService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/templates")
public class TemplateCatalogController {

    private final TemplateCatalogService catalogService;

    public TemplateCatalogController(TemplateCatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @LogActivity("List templates available in the public catalog")
    @GetMapping
    public ResponseEntity<TemplateCatalogPageResponse> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(required = false) MemoryType memoryType,
            @RequestParam(required = false) TemplateStatus status
    ) {
        return ResponseEntity.ok(catalogService.list(page, size, memoryType, status));
    }
}
