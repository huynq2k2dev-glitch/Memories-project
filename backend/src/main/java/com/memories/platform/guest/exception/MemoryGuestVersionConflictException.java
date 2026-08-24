package com.memories.platform.guest.exception;

public class MemoryGuestVersionConflictException extends RuntimeException {

    private final Long currentVersion;

    public MemoryGuestVersionConflictException(Long currentVersion) {
        this.currentVersion = currentVersion;
    }

    public Long getCurrentVersion() {
        return currentVersion;
    }
}
