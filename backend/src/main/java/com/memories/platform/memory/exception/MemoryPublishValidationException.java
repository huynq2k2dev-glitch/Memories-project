package com.memories.platform.memory.exception;

public class MemoryPublishValidationException extends RuntimeException {

    private final String code;
    private final String safeDetail;

    public MemoryPublishValidationException(String code, String safeDetail) {
        this.code = code;
        this.safeDetail = safeDetail;
    }

    public String getCode() {
        return code;
    }

    public String getSafeDetail() {
        return safeDetail;
    }
}
