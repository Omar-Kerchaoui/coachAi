package com.lose10.platform.error;

/**
 * Stable, client-facing error codes. Never expose raw exception messages or
 * stack traces — every error returned to a client maps to one of these.
 */
public enum ErrorCode {
    VALIDATION_FAILED,
    RESOURCE_NOT_FOUND,
    RESOURCE_CONFLICT,
    UNAUTHENTICATED,
    UNAUTHORIZED,
    RATE_LIMITED,
    INVALID_CREDENTIALS,
    TOKEN_EXPIRED,
    TOKEN_REUSE_DETECTED,
    EMAIL_ALREADY_REGISTERED,
    EMAIL_NOT_VERIFIED,
    BUSINESS_RULE_VIOLATION,
    INTERNAL_ERROR
}
