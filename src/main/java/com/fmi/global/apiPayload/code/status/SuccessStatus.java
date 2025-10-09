package com.fmi.global.apiPayload.code.status;


import com.fmi.global.apiPayload.code.BaseCode;
import com.fmi.global.apiPayload.code.ReasonDTO;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum SuccessStatus implements BaseCode {

    // 일반적인 응답
    _OK(HttpStatus.OK, "COMMON200", "성공입니다."),

    // 채팅방 관련 응답
    _CHATROOM_CREATED(HttpStatus.CREATED, "CHATROOM201_CREATED", "채팅방이 생성되었습니다."),
    _CHATROOM_FOUND(HttpStatus.OK, "CHATROOM200_FOUND", "기존 채팅방을 조회합니다."),
    _CHATROOM_LIST_FETCHED(HttpStatus.OK, "CHATROOM200_LIST", "나의 채팅 목록 조회에 성공했습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    @Override
    public ReasonDTO getReason() {
        return ReasonDTO.builder()
                .message(message)
                .code(code)
                .isSuccess(true)
                .build();
    }

    @Override
    public ReasonDTO getReasonHttpStatus() {
        return ReasonDTO.builder()
                .message(message)
                .code(code)
                .isSuccess(true)
                .httpStatus(httpStatus)
                .build();
    }

}
