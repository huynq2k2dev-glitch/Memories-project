package com.memories.platform.memory.constants;

import com.memories.platform.memory.entity.MemoryVisibility;

import java.util.Set;

public final class MemoryPublishingConstants {

    public static final String AUDIT_ACTION = "MEMORY_PUBLISHED";
    public static final String AUDIT_ENTITY_TYPE = "MEMORY";
    public static final Set<MemoryVisibility> PUBLIC_VISIBILITIES = Set.of(
            MemoryVisibility.PUBLIC,
            MemoryVisibility.UNLISTED
    );
    public static final Set<MemoryVisibility> PUBLISHABLE_VISIBILITIES = Set.of(
            MemoryVisibility.PUBLIC,
            MemoryVisibility.UNLISTED,
            MemoryVisibility.PRIVATE,
            MemoryVisibility.PASSWORD_PROTECTED
    );

    private MemoryPublishingConstants() {
    }
}
