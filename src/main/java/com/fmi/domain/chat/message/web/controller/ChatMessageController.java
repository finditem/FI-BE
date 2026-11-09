package com.fmi.domain.chat.message.web.controller;

import static com.fmi.domain.chat.message.web.dto.request.ChatMessageRequest.SendMessageRequest;

import com.fmi.domain.chat.message.service.ChatMessageQueryService;
import com.fmi.domain.chat.message.service.ChatMessageService;
import com.fmi.domain.chat.message.web.dto.response.ChatMessageResponse;
import com.fmi.domain.chat.message.web.swagger.ChatMessageSwagger;
import com.fmi.domain.user.user.data.User;
import com.fmi.domain.user.user.service.UserQueryService;
import com.fmi.global.apiPayload.ApiResponse;
import com.fmi.global.apiPayload.code.status.SuccessStatus;
import java.security.Principal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/chats")
@Slf4j
public class ChatMessageController implements ChatMessageSwagger {
    private final ChatMessageQueryService chatMessageQueryService;

    private final ChatMessageService chatMessageService;
    private final UserQueryService userService;

    @MessageMapping("/chats/{roomId}/send")
    public void sendMessage(
            @DestinationVariable Long roomId, Principal principal, @Payload SendMessageRequest requestDTO) {
        Long userId = Long.parseLong(principal.getName());
        log.info("메세지 보낸 userId = {}", userId);
        chatMessageService.sendMessage(roomId, userId, requestDTO);
    }

    @PostMapping(value = "/{roomId}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Override
    public ApiResponse<Void> uploadImage(
            @PathVariable Long roomId,
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestPart("images") List<MultipartFile> images) {

        String email = userDetails.getUsername();
        User user = userService.findUser(email);

        chatMessageService.sendImageMessage(roomId, images, user.getId());
        return ApiResponse.of(SuccessStatus._MESSAGE_IMAGE_SENT);
    }

    @GetMapping("/{roomId}/messages")
    @Override
    public ApiResponse<ChatMessageResponse.MessageSliceResponse> listMessages(
            @PathVariable Long roomId,
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) Long cursor) {
        String email = userDetails.getUsername();
        User user = userService.findUser(email);
        ChatMessageResponse.MessageSliceResponse messageSliceResponseDTO =
                chatMessageQueryService.messageSlice(roomId, user.getId(), cursor);
        return ApiResponse.of(SuccessStatus._MESSAGE_LIST_FETCHED, messageSliceResponseDTO);
    }

    @PatchMapping("/{roomId}/read")
    @Override
    public ApiResponse<Void> readMessages(@PathVariable Long roomId, @AuthenticationPrincipal UserDetails userDetails) {
        String email = userDetails.getUsername();
        User user = userService.findUser(email);
        chatMessageService.readMessages(roomId, user.getId());
        return ApiResponse.of(SuccessStatus._MESSAGE_READ);
    }
}
