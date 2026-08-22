package com.memories.platform.memory.constants;

import java.util.Set;

public final class MemoryScheduleConstants {

    public static final String DEFAULT_TIMEZONE = "Asia/Ho_Chi_Minh";
    public static final Set<String> DIRECT_MAP_HOSTS = Set.of(
            "maps.google.com",
            "maps.app.goo.gl",
            "www.openstreetmap.org",
            "openstreetmap.org"
    );

    private MemoryScheduleConstants() {
    }
}
