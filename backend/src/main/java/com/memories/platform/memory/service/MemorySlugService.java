package com.memories.platform.memory.service;

import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.Locale;
import java.util.UUID;

@Service
public class MemorySlugService {

    private static final int MAXIMUM_BASE_LENGTH = 147;

    public String generate(String title, UUID memoryId) {
        String normalizedTitle = Normalizer.normalize(title, Normalizer.Form.NFD)
                .replace("đ", "d")
                .replace("Đ", "D")
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-+|-+$)", "");
        if (normalizedTitle.isBlank()) {
            normalizedTitle = "memory";
        }
        String base = normalizedTitle.substring(0, Math.min(normalizedTitle.length(), MAXIMUM_BASE_LENGTH))
                .replaceAll("-+$", "");
        return base + "-" + memoryId.toString().replace("-", "");
    }
}
