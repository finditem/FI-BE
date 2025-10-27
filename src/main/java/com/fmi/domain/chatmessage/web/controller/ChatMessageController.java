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
public class ChatMessageController {

    private final ChatMessageService chatMessageService;
    private final UserQueryService userService;

    @MessageMapping("/{roomId}/send")
    public void sendMessage(@DestinationVariable Long roomId, Principal principal, @Payload SendMessageRequestDTO requestDTO) {
        String email = principal.getName();
        User user = userService.findUser(email);
        chatMessageService.sendMessage(roomId, user.getId(), requestDTO);
    }

    @PostMapping(value = "/{roomId}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<Void> uploadImage(@PathVariable Long roomId,
                                         @AuthenticationPrincipal UserDetails userDetails, @ModelAttribute SendImageRequestDTO requestDTO) {

        String email = userDetails.getUsername();
        User user = userService.findUser(email);

        chatMessageService.sendImageMessage(roomId, requestDTO, user.getId());
        return ApiResponse.of(SuccessStatus._OK);
    }

    @Operation(summary = "이전 채팅 내역 조회", description = "현재 채팅방의 이전 대화 내역을 커서 기반 페이지네이션으로 조회합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "MESSAGE_LIST_FETCHED: 이전 채팅 내역 조회 성공")
    })
    @Parameters({
            @Parameter(name = "roomId", description = "조회할 채팅방의 ID", required = true),
            @Parameter(name = "cursor", description = "이전 페이지 응답의 nextCursor 값. 첫 페이지 조회 시 생략.", required = false)
    })
    @GetMapping("{roomId}/messages")
    public ApiResponse<ChatMessageResponseDTO.MessageSliceResponseDTO> listMessages(@PathVariable Long roomId, @AuthenticationPrincipal UserDetails userDetails, @RequestParam(required = false) Long cursor) {
        String email = userDetails.getUsername();
        User user = userService.findUser(email);
        ChatMessageResponseDTO.MessageSliceResponseDTO messageSliceResponseDTO = chatMessageService.messageSlice(roomId, user.getId(), cursor);
        return ApiResponse.of(SuccessStatus._MESSAGE_LIST_FETCHED,messageSliceResponseDTO);
    }
}
