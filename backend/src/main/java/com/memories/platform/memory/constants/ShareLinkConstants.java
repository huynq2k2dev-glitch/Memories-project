package com.memories.platform.memory.constants;

public final class ShareLinkConstants {

    public static final int TOKEN_BYTES = 32;
    public static final int TOKEN_LENGTH = 43;
    public static final String PUBLIC_PATH_PREFIX = "/shares/";
    public static final String MEMORY_PATH_PREFIX = "/memories/";
    public static final String AUDIT_ENTITY_TYPE = "SHARE_LINK";
    public static final String AUDIT_CREATE_ACTION = "SHARE_LINK_CREATE";
    public static final String AUDIT_REVOKE_ACTION = "SHARE_LINK_REVOKE";

    private ShareLinkConstants() {
    }
}
