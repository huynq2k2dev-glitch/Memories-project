package com.memories.platform.memory.service;

import com.memories.platform.memory.exception.UnsafeMemoryContentException;
import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

@Service
public class MemoryContentSafetyService {

    private static final Pattern RAW_HTML = Pattern.compile(
            "<\\s*(?:/?[A-Za-z][^>]*|!--)",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern DANGEROUS_PROTOCOL = Pattern.compile(
            "(?:javascript\\s*:|data\\s*:\\s*text/html)",
            Pattern.CASE_INSENSITIVE
    );

    public void requireSafeMarkdown(String value) {
        if (value == null) {
            return;
        }
        if (RAW_HTML.matcher(value).find() || DANGEROUS_PROTOCOL.matcher(value).find()) {
            throw new UnsafeMemoryContentException();
        }
    }
}
