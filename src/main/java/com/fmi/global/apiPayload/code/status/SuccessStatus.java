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
    _CHATROOM_CREATED(HttpStatus.CREATED, "CHATROOM201-CREATED", "채팅방이 생성되었습니다."),
    _CHATROOM_FOUND(HttpStatus.OK, "CHATROOM200-FOUND", "기존 채팅방을 조회합니다."),
    _CHATROOM_LIST_FETCHED(HttpStatus.OK, "CHATROOM200-LIST", "나의 채팅 목록 조회에 성공했습니다."),
    _CHATROOM_LEFT(HttpStatus.OK, "CHATROOM200-LEFT", "채팅방 나가기를 성공했습니다."),

    // 메세지 관련 응답
    _MESSAGE_LIST_FETCHED(HttpStatus.OK, "MESSAGE200-LIST", "이전 채팅 내역 조회를 성공했습니다."),
    _MESSAGE_READ(HttpStatus.OK, "MESSAGE200-READ", "메세지 읽음을 성공했습니다."),
    _MESSAGE_IMAGE_SENT(HttpStatus.OK, "MESSAGE200-SENT", "이미지 전송에 성공하였습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    @Override
    public ReasonDTO getReason() {
        return ReasonDTO.builder().message(message).code(code).isSuccess(true).build();
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
