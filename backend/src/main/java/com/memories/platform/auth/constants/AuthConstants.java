package com.memories.platform.auth.constants;

public final class AuthConstants {

    public static final String DEFAULT_ROLE = "USER";
    public static final String PERMISSION_USER_MANAGE = "USER_MANAGE";
    public static final String DEFAULT_LOCALE = "vi-VN";
    public static final String DEFAULT_TIMEZONE = "Asia/Ho_Chi_Minh";
    public static final int VERIFICATION_TOKEN_BYTES = 32;
    public static final int REFRESH_TOKEN_BYTES = 32;
    public static final String AUDIT_LOGIN = "LOGIN";
    public static final String AUDIT_REFRESH = "REFRESH";
    public static final String AUDIT_LOGOUT = "LOGOUT";
    public static final String AUDIT_ACCOUNT_LOCK = "ACCOUNT_LOCK";
    public static final String AUDIT_ACCESS_DENIED = "ACCESS_DENIED";
    public static final String AUDIT_SUCCESS = "SUCCESS";
    public static final String AUDIT_FAILURE = "FAILURE";
    public static final String REASON_INVALID_CREDENTIALS = "INVALID_CREDENTIALS";
    public static final String REASON_EMAIL_NOT_VERIFIED = "EMAIL_NOT_VERIFIED";
    public static final String REASON_ACCOUNT_LOCKED = "ACCOUNT_LOCKED";
    public static final String REASON_RATE_LIMITED = "RATE_LIMITED";
    public static final String REASON_PERMISSION_DENIED = "PERMISSION_DENIED";
    public static final String REASON_SESSION_INVALID = "SESSION_INVALID";
    public static final String REASON_REFRESH_TOKEN_REUSE = "REFRESH_TOKEN_REUSE";
    public static final String REASON_ACCOUNT_NOT_FOUND = "ACCOUNT_NOT_FOUND";

    public static final String REFRESH_REVOKED_ROTATED = "ROTATED";
    public static final String REFRESH_REVOKED_REUSE = "REUSE_DETECTED";
    public static final String REFRESH_REVOKED_LOGOUT = "LOGOUT";
    public static final String REFRESH_REVOKED_LOGOUT_ALL = "LOGOUT_ALL";
    public static final String REFRESH_REVOKED_ACCOUNT_INACTIVE = "ACCOUNT_INACTIVE";
    public static final String REFRESH_REVOKED_EXPIRED = "EXPIRED";
    public static final String REFRESH_REVOKED_ACCOUNT_LOCKED = "ACCOUNT_LOCKED";

    private AuthConstants() {
    }
}
