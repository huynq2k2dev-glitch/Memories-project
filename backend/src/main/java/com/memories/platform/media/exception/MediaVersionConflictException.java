package com.memories.platform.media.exception;

public class MediaVersionConflictException extends RuntimeException {

    private final Long currentVersion;

    public MediaVersionConflictException(Long currentVersion) {
        this.currentVersion = currentVersion;
    }

    public Long getCurrentVersion() {
        return currentVersion;
    }
}
