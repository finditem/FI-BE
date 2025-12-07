package com.fmi.domain.chatmessage.web.controller;

import com.fmi.domain.auth.data.User;
import com.fmi.domain.chatmessage.service.ChatMessageService;
import com.fmi.domain.chatmessage.web.dto.ChatMessageResponseDTO;
import com.fmi.global.apiPayload.ApiResponse;
import com.fmi.global.apiPayload.code.status.SuccessStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import com.fmi.service.UserQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

import static com.fmi.domain.chatmessage.web.dto.ChatMessageRequestDTO.SendImageRequestDTO;
import static com.fmi.domain.chatmessage.web.dto.ChatMessageRequestDTO.SendMessageRequestDTO;

@RestController
@RequiredArgsConstructor
@RequestMapping("/chats")
@Slf4j
public class ChatMessageController {

    private final ChatMessageService chatMessageService;
    private final UserQueryService userService;

    @MessageMapping("/chats/{roomId}/send")
    public void sendMessage(@DestinationVariable Long roomId, Principal principal, @Payload SendMessageRequestDTO requestDTO) {
        Long userId = Long.parseLong(principal.getName());
        log.info("메세지 보낸 userId = {}", userId);
        chatMessageService.sendMessage(roomId, userId, requestDTO);
    }

    @PostMapping(value = "/{roomId}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "채팅 이미지 전송", description = "채팅방에 이미지를 전송합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "이미지 전송 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "IMAGE400-NOT_PROVIDED: 전송할 이미지가 없습니다"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "FILE400-EXT_MISSING: 확장자가 존재하지 않습니다"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "COMMON401: 인증이 필요합니다"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "MESSAGE-NOT_ALLOWED: 채팅 내역을 조회할 권한이 없습니다"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "CHATROOM404-NOT_FOUND: 존재하지 않는 채팅방입니다"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "415", description = "FILE415-EXT_UNSUPPORTED: 허용되지 않는 확장자입니다"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "FILE500-UPLOAD_IO: 업로드 중 오류가 발생했습니다"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "COMMON500: 서버 에러")
    })
    public ApiResponse<Void> uploadImage(@PathVariable Long roomId,
                                         @AuthenticationPrincipal UserDetails userDetails, @ModelAttribute SendImageRequestDTO requestDTO) {

        String email = userDetails.getUsername();
        User user = userService.findUser(email);

        chatMessageService.sendImageMessage(roomId, requestDTO, user.getId());
        return ApiResponse.of(SuccessStatus._OK);
    }

    @Operation(summary = "이전 채팅 내역 조회", description = "현재 채팅방의 이전 대화 내역을 커서 기반 페이지네이션으로 조회합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "MESSAGE_LIST_FETCHED: 이전 채팅 내역 조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "LIST400-INVALID_CURSOR: 유효하지 않은 커서입니다"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "COMMON401: 인증이 필요합니다"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "MESSAGE-NOT_ALLOWED: 채팅 내역을 조회할 권한이 없습니다"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "CHATROOM404-NOT_FOUND: 존재하지 않는 채팅방입니다"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "COMMON500: 서버 에러")
    })
    @Parameters({
            @Parameter(name = "roomId", description = "조회할 채팅방의 ID", required = true),
            @Parameter(name = "cursor", description = "이전 페이지 응답의 nextCursor 값. 첫 페이지 조회 시 생략.", required = false)
    })
    @GetMapping("/{roomId}/messages")
    public ApiResponse<ChatMessageResponseDTO.MessageSliceResponseDTO> listMessages(@PathVariable Long roomId, @AuthenticationPrincipal UserDetails userDetails, @RequestParam(required = false) Long cursor) {
        String email = userDetails.getUsername();
        User user = userService.findUser(email);
        ChatMessageResponseDTO.MessageSliceResponseDTO messageSliceResponseDTO = chatMessageService.messageSlice(roomId, user.getId(), cursor);
        return ApiResponse.of(SuccessStatus._MESSAGE_LIST_FETCHED,messageSliceResponseDTO);
    }

    @Operation(summary = "채팅방 메시지 읽음 처리",
            description = "유저가 특정 채팅방에 입장할 때 호출하여 쌓인 메시지를 모두 읽음 처리합니다. \n" +
                    "1. 해당 유저의 unreadCount를 0으로 갱신하고 lastReadMessageId를 최신으로 업데이트합니다. \n" +
                    "2. (WebSocket) 상대방에게는 '/queue/read-receipts'로 실시간 읽음 확인 이벤트를 전송합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "MESSAGE200-READ, 메세지 읽음을 성공했습니다."),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "COMMON401: 인증이 필요합니다"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "MESSAGE-NOT_ALLOWED: 채팅 내역을 조회할 권한이 없습니다"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "CHATROOM404-NOT_FOUND: 존재하지 않는 채팅방입니다"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "COMMON500: 서버 에러")
    })
    @Parameters({
            @Parameter(name = "roomId", description = "읽을 채팅방의 ID", required = true)
    })
    @PatchMapping("/{roomId}/read")
    public ApiResponse<Void> readMessages(@PathVariable Long roomId, @AuthenticationPrincipal UserDetails userDetails) {
        String email = userDetails.getUsername();
        User user = userService.findUser(email);
        chatMessageService.readMessages(roomId, user.getId());
        return ApiResponse.of(SuccessStatus._MESSAGE_READ);
    }

}
