package com.memories.platform.media.constants;

import java.util.Map;
import java.util.Set;

public final class MediaConstants {

    public static final Set<String> ALLOWED_IMAGE_MIME_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp",
            "image/avif"
    );

    public static final Map<String, String> FILE_EXTENSIONS = Map.of(
            "image/jpeg", ".jpg",
            "image/png", ".png",
            "image/webp", ".webp",
            "image/avif", ".avif"
    );

    private MediaConstants() {
    }
}
