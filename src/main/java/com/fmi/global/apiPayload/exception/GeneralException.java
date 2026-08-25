package com.fmi.global.apiPayload.exception;

import com.fmi.global.apiPayload.code.BaseErrorCode;
import com.fmi.global.apiPayload.code.ErrorReasonDTO;
import java.util.Map;
import lombok.Getter;

@Getter
public class GeneralException extends RuntimeException {

    private final BaseErrorCode code;
    private final Map<String, Object> errorArgs;

    public GeneralException(BaseErrorCode code) {
        this(code, null);
    }

    public GeneralException(BaseErrorCode code, Map<String, Object> errorArgs) {
        this.code = code;
        this.errorArgs = errorArgs;
    }

    public ErrorReasonDTO getErrorReason() {
        return this.code.getReason();
    }

    public ErrorReasonDTO getErrorReasonHttpStatus() {
        return this.code.getReasonHttpStatus();
    }
}
