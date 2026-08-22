package com.memories.platform.memory.exception;

public class MemoryVersionConflictException extends RuntimeException {

    private final Long currentVersion;

    public MemoryVersionConflictException(Long currentVersion) {
        this.currentVersion = currentVersion;
    }

    public Long getCurrentVersion() {
        return currentVersion;
    }
}
