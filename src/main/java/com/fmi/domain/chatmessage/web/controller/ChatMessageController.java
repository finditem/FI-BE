package com.fmi.domain.chatmessage.web.controller;

import com.fmi.domain.auth.data.User;
import com.fmi.domain.chatmessage.service.ChatMessageService;
import com.fmi.global.apiPayload.ApiResponse;
import com.fmi.global.apiPayload.code.status.SuccessStatus;
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
}
