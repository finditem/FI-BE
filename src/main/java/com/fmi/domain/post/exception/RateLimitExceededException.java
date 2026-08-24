package com.fmi.domain.post.exception;

import com.fmi.global.apiPayload.code.BaseErrorCode;
import lombok.Getter;

@Getter
public class RateLimitExceededException extends RuntimeException {

    private final BaseErrorCode errorCode;
    private final long retryAfterSeconds;

    public RateLimitExceededException(BaseErrorCode errorCode, long retryAfterSeconds) {
        this.errorCode = errorCode;
        this.retryAfterSeconds = retryAfterSeconds;
    }
}
