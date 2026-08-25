package com.lose10.platform.error;

import java.util.List;

/**
 * Uniform error envelope returned to API clients, per docs/API.md.
 */
public record ApiError(
        ErrorCode code,
        String message,
        String requestId,
        List<String> details
) {
    public static ApiError of(ErrorCode code, String message, String requestId) {
        return new ApiError(code, message, requestId, List.of());
    }

    public static ApiError of(ErrorCode code, String message, String requestId, List<String> details) {
        return new ApiError(code, message, requestId, details);
    }
}
