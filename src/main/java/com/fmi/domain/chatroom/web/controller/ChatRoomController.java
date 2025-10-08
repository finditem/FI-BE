package com.fmi.domain.chatroom.web.controller;

import com.fmi.domain.User;
import com.fmi.domain.chatroom.service.ChatRoomService;
import com.fmi.domain.chatroom.web.dto.ChatRoomResponseDTO;
import com.fmi.global.apiPayload.ApiResponse;
import com.fmi.global.apiPayload.code.status.SuccessStatus;
import com.fmi.security.CustomUserDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.util.Pair;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import static com.fmi.domain.chatroom.web.dto.ChatRoomResponseDTO.ChatRoomResultDTO;

@RestController
@RequiredArgsConstructor
public class ChatRoomController {

    private final CustomUserDetailsService customUserDetailsService;
    private final ChatRoomService chatRoomService;

    @PostMapping("/posts/{postId}/chats")
    public ApiResponse<ChatRoomResultDTO> createChatRoom(@PathVariable("postId") Long postId, @AuthenticationPrincipal UserDetails userDetails) {
        String email = userDetails.getUsername();
        User user = customUserDetailsService.findUser(email);
        Pair<ChatRoomResultDTO, Boolean> result = chatRoomService.createChatRoom(postId, user.getUserId());

        ChatRoomResultDTO responseDTO = result.getFirst();
        boolean isNew = result.getSecond();

        if (isNew) {
            // 새로 생성된 경우
            return ApiResponse.of(SuccessStatus._CHATROOM_CREATED, responseDTO);
        } else {
            // 기존에 존재했던 경우
            return ApiResponse.of(SuccessStatus._CHATROOM_FOUND, responseDTO);
        }

    }

    @GetMapping("/posts/{postId}/my-chats")
    public ApiResponse<ChatRoomResponseDTO.MyPostChatListDTO> getMyPostChatRooms(
            @PathVariable Long postId,
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        User user = customUserDetailsService.findUser(userDetails.getUsername());
        Long ownerId = user.getUserId();

        ChatRoomResponseDTO.MyPostChatListDTO responseDTO = chatRoomService.getMyPostChatRooms(postId, ownerId, cursor, size);
        return ApiResponse.onSuccess(responseDTO);
    }

}
