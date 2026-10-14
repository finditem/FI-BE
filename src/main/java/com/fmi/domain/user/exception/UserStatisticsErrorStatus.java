package com.fmi.domain.user.exception;

import com.fmi.global.apiPayload.code.BaseErrorCode;
import com.fmi.global.apiPayload.code.ErrorReasonDTO;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum UserStatisticsErrorStatus implements BaseErrorCode {
    INVALID_DATE_RANGE(HttpStatus.BAD_REQUEST, "ADMIN_STATISTICS400-INVALID_DATE_RANGE", "시작일은 종료일보다 늦을 수 없습니다."),
    BEFORE_AVAILABLE_DATE(
            HttpStatus.BAD_REQUEST, "ADMIN_STATISTICS400-BEFORE_AVAILABLE_DATE", "통계 도입일 이전은 조회할 수 없습니다."),
    FUTURE_END_DATE(HttpStatus.BAD_REQUEST, "ADMIN_STATISTICS400-FUTURE_END_DATE", "미래 날짜는 조회할 수 없습니다."),
    NOT_INITIALIZED(HttpStatus.SERVICE_UNAVAILABLE, "ADMIN_STATISTICS503-NOT_INITIALIZED", "통계가 초기화되지 않았습니다.");
    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    @Override
    public ErrorReasonDTO getReason() {
        return ErrorReasonDTO.builder()
                .isSuccess(false)
                .code(code)
                .message(message)
                .build();
    }

    @Override
    public ErrorReasonDTO getReasonHttpStatus() {
        return ErrorReasonDTO.builder()
                .isSuccess(false)
                .code(code)
                .message(message)
                .httpStatus(httpStatus)
                .build();
    }
}
