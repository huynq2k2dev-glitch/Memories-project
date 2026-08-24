package com.memories.platform.template.constants;

import java.util.Map;
import java.util.Set;

public final class TemplateConstants {

    public static final String PERMISSION_TEMPLATE_MANAGE = "TEMPLATE_MANAGE";
    public static final String AUDIT_TEMPLATE_MANAGE = "TEMPLATE_MANAGE";
    public static final String COMPONENT_MEMORIES_BASIC_V1 = "memories-basic-v1";
    public static final String RENDERER_VERSION_1 = "1";
    public static final int MAXIMUM_PAGE_SIZE = 50;
    public static final Map<String, Set<String>> ALLOWED_RENDERERS = Map.of(
            COMPONENT_MEMORIES_BASIC_V1,
            Set.of(RENDERER_VERSION_1)
    );

    private TemplateConstants() {
    }
}
