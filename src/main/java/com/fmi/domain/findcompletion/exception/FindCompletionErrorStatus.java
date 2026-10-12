package com.fmi.domain.findcompletion.exception;

import com.fmi.global.apiPayload.code.BaseErrorCode;
import com.fmi.global.apiPayload.code.ErrorReasonDTO;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum FindCompletionErrorStatus implements BaseErrorCode {
    INVALID_POST_TYPE(HttpStatus.BAD_REQUEST, "FIND_COMPLETION400-INVALID_POST_TYPE", "분실물 게시글만 찾기 완료를 처리할 수 있습니다."),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "FIND_COMPLETION403-ACCESS_DENIED", "게시글 작성자만 찾기 완료를 처리할 수 있습니다."),
    NOT_FOUND(HttpStatus.NOT_FOUND, "FIND_COMPLETION404-NOT_FOUND", "찾기 완료 기록을 찾을 수 없습니다."),
    ALREADY_COMPLETED(HttpStatus.CONFLICT, "FIND_COMPLETION409-ALREADY_COMPLETED", "이미 찾기 완료된 게시글입니다.");

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
