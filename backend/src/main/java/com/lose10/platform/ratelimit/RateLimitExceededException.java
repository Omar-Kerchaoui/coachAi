package com.lose10.platform.ratelimit;

import com.lose10.platform.error.ApiException;
import com.lose10.platform.error.ErrorCode;
import org.springframework.http.HttpStatus;

public class RateLimitExceededException extends ApiException {
    public RateLimitExceededException() {
        super(ErrorCode.RATE_LIMITED, HttpStatus.TOO_MANY_REQUESTS, "Too many requests — please try again later");
    }
}
