package com.memories.platform.guest.constants;

public final class GuestMessageConstants {

    public static final int MAX_PAGE_SIZE = 50;
    public static final String AUDIT_ENTITY_TYPE = "GUEST_MESSAGE";
    public static final String AUDIT_SUBMIT_ACTION = "GUEST_MESSAGE_SUBMITTED";
    public static final String AUDIT_APPROVE_ACTION = "GUEST_MESSAGE_APPROVED";
    public static final String AUDIT_REJECT_ACTION = "GUEST_MESSAGE_REJECTED";
    public static final String AUDIT_HIDE_ACTION = "GUEST_MESSAGE_HIDDEN";
    public static final String AUDIT_REQUEUE_ACTION = "GUEST_MESSAGE_REQUEUED";
    public static final String AUDIT_RATE_LIMIT_ACTION = "GUEST_MESSAGE_RATE_LIMITED";

    private GuestMessageConstants() {
    }
}
