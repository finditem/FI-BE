package com.fmi.domain.chat.message.web.swagger;

import com.fmi.domain.chat.message.web.dto.response.ChatMessageResponse;
import com.fmi.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "ChatMessage", description = "채팅 메시지 관련 API")
public interface ChatMessageSwagger {

    @Operation(summary = "채팅 이미지 전송", description = "채팅방에 이미지를 전송합니다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "MESSAGE-SENT: 이미지 전송에 성공하였습니다."),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "FILE-EXT_MISSING: 확장자가 존재하지 않습니다"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "415",
                description = "FILE-EXT_UNSUPPORTED: 허용되지 않는 확장자입니다"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "500",
                description = "FILE-UPLOAD_IO: 업로드 중 오류가 발생했습니다")
    })
    ApiResponse<Void> uploadImage(
            Long roomId, @Parameter(hidden = true) UserDetails userDetails, List<MultipartFile> images);

    @Operation(summary = "이전 채팅 내역 조회", description = "현재 채팅방의 이전 대화 내역을 커서 기반 페이지네이션으로 조회합니다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "MESSAGE_LIST_FETCHED: 이전 채팅 내역 조회를 성공했습니다."),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "403",
                description = "MESSAGE-NOT_ALLOWED: 채팅 내역을 조회할 권한이 없습니다")
    })
    @Parameters({
        @Parameter(name = "roomId", description = "조회할 채팅방의 ID", required = true),
        @Parameter(name = "cursor", description = "이전 페이지 응답의 nextCursor 값. 첫 페이지 조회 시 생략.", required = false)
    })
    ApiResponse<ChatMessageResponse.MessageSliceResponse> listMessages(
            Long roomId, @Parameter(hidden = true) UserDetails userDetails, Long cursor);

    @Operation(
            summary = "채팅방 메시지 읽음 처리",
            description = "유저가 특정 채팅방에 입장할 때 호출하여 쌓인 메시지를 모두 읽음 처리합니다. \n"
                    + "1. 해당 유저의 unreadCount를 0으로 갱신하고 lastReadMessageId를 최신으로 업데이트합니다. \n"
                    + "2. (WebSocket) 상대방에게는 '/queue/read-receipts'로 실시간 읽음 확인 이벤트를 전송합니다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "MESSAGE-READ: 메세지 읽음을 성공했습니다."),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "403",
                description = "CHATROOM-PARTICIPANT_NOT_FOUND: 참여 중인 채팅방이 아닙니다."),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "CHATROOM-NOT_FOUND: 존재하지 않는 채팅방입니다.")
    })
    @Parameters({@Parameter(name = "roomId", description = "읽을 채팅방의 ID", required = true)})
    ApiResponse<Void> readMessages(Long roomId, @Parameter(hidden = true) UserDetails userDetails);
}
