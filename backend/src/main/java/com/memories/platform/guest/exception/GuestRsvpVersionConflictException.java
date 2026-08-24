package com.memories.platform.guest.exception;

public class GuestRsvpVersionConflictException extends RuntimeException {

    private final Long currentVersion;

    public GuestRsvpVersionConflictException(Long currentVersion) {
        this.currentVersion = currentVersion;
    }

    public Long getCurrentVersion() {
        return currentVersion;
    }
}
