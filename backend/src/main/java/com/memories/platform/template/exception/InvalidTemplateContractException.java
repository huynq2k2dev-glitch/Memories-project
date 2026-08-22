package com.memories.platform.template.exception;

public class InvalidTemplateContractException extends RuntimeException {

    private final String code;
    private final String safeDetail;

    public InvalidTemplateContractException(String code, String safeDetail) {
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
