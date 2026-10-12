package com.fmi.domain.review.exception;

import com.fmi.global.apiPayload.code.BaseErrorCode;
import com.fmi.global.apiPayload.code.ErrorReasonDTO;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ReviewErrorStatus implements BaseErrorCode {
    INVALID_CONTENT(HttpStatus.BAD_REQUEST, "REVIEW400-INVALID_CONTENT", "상세 후기는 최대 300자까지 작성할 수 있습니다."),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "REVIEW403-ACCESS_DENIED", "해당 후기에 대한 권한이 없습니다."),
    NOT_FOUND(HttpStatus.NOT_FOUND, "REVIEW404-NOT_FOUND", "후기를 찾을 수 없습니다."),
    ALREADY_WRITTEN(HttpStatus.CONFLICT, "REVIEW409-ALREADY_WRITTEN", "이미 후기를 작성했습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    @Override
    public ErrorReasonDTO getReason() {
        return ErrorReasonDTO.builder()
                .message(message)
                .code(code)
                .isSuccess(false)
                .build();
    }

    @Override
    public ErrorReasonDTO getReasonHttpStatus() {
        return ErrorReasonDTO.builder()
                .message(message)
                .code(code)
                .isSuccess(false)
                .httpStatus(httpStatus)
                .build();
    }
}
