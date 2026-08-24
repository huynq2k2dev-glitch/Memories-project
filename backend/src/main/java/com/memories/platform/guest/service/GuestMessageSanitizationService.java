package com.memories.platform.guest.service;

import com.memories.platform.guest.exception.UnsafeGuestMessageException;
import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

@Service
public class GuestMessageSanitizationService {

    private static final Pattern RAW_HTML = Pattern.compile(
            "<\\s*(?:/?[A-Za-z][^>]*|!--)",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern DANGEROUS_PROTOCOL = Pattern.compile(
            "(?:javascript\\s*:|data\\s*:\\s*text/html)",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    public String guestName(String value) {
        String normalized = plainText(value);
        normalized = WHITESPACE.matcher(normalized).replaceAll(" ").trim();
        if (normalized.isEmpty() || normalized.length() > 200) {
            throw new UnsafeGuestMessageException();
        }
        return normalized;
    }

    public String content(String value) {
        String normalized = plainText(value).trim();
        if (normalized.isEmpty() || normalized.length() > 2000) {
            throw new UnsafeGuestMessageException();
        }
        return normalized;
    }

    private String plainText(String value) {
        if (value == null
                || RAW_HTML.matcher(value).find()
                || DANGEROUS_PROTOCOL.matcher(value).find()) {
            throw new UnsafeGuestMessageException();
        }

        String lineNormalized = value.replace("\r\n", "\n").replace('\r', '\n');
        StringBuilder sanitized = new StringBuilder(lineNormalized.length());
        for (int index = 0; index < lineNormalized.length(); index++) {
            char character = lineNormalized.charAt(index);
            if (!Character.isISOControl(character)
                    || character == '\n'
                    || character == '\t') {
                sanitized.append(character);
            }
        }
        return sanitized.toString();
    }
}
