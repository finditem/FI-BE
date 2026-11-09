package com.fmi.domain.chat.room.web.controller;

import static com.fmi.domain.chat.room.web.dto.response.ChatRoomResponse.ChatRoomResultResponse;
import static com.fmi.domain.chat.room.web.dto.response.ChatRoomResponse.MyChatListResponse;

import com.fmi.domain.chat.message.service.ChatNotificationService;
import com.fmi.domain.chat.room.service.ChatRoomPresenceService;
import com.fmi.domain.chat.room.service.ChatRoomQueryService;
import com.fmi.domain.chat.room.service.ChatRoomService;
import com.fmi.domain.chat.room.web.swagger.ChatRoomSwagger;
import com.fmi.domain.post.post.data.enums.PostType;
import com.fmi.domain.user.user.data.User;
import com.fmi.domain.user.user.service.UserQueryService;
import com.fmi.global.apiPayload.ApiResponse;
import com.fmi.global.apiPayload.code.status.SuccessStatus;
import com.fmi.global.enums.SortType;
import java.security.Principal;
import lombok.RequiredArgsConstructor;
import org.springframework.data.util.Pair;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class ChatRoomController implements ChatRoomSwagger {
    private final ChatRoomQueryService chatRoomQueryService;

    private final UserQueryService userService;
    private final ChatRoomService chatRoomService;
    private final ChatRoomPresenceService presenceService;
    private final ChatNotificationService chatNotificationService;

    @PostMapping("/posts/{postId}/chats")
    @Override
    public ResponseEntity<ApiResponse<ChatRoomResultResponse>> createChatRoom(
            @PathVariable("postId") Long postId, @AuthenticationPrincipal UserDetails userDetails) {
        String email = userDetails.getUsername();
        User user = userService.findUser(email);
        Pair<ChatRoomResultResponse, Boolean> result = chatRoomService.createChatRoom(postId, user.getId());

        ChatRoomResultResponse responseDTO = result.getFirst();
        boolean isNew = result.getSecond();

        if (isNew) {
            // 새로 생성된 경우
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.of(SuccessStatus._CHATROOM_CREATED, responseDTO));
        } else {
            // 기존에 존재했던 경우
            return ResponseEntity.status(HttpStatus.OK)
                    .body(ApiResponse.of(SuccessStatus._CHATROOM_FOUND, responseDTO));
        }
    }

    @GetMapping("/users/me/chats")
    @Override
    public ApiResponse<MyChatListResponse> getMyPostChatRooms(
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(name = "type", required = false) PostType type,
            @RequestParam(name = "address", required = false) String address,
            @RequestParam(name = "sort", required = false, defaultValue = "LATEST") SortType sort,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.findUser(userDetails.getUsername());

        MyChatListResponse responseDTO =
                chatRoomQueryService.getMyPostChatRooms(user, cursor, size, type, address, sort);
        return ApiResponse.of(SuccessStatus._CHATROOM_LIST_FETCHED, responseDTO);
    }

    @PostMapping("/chats/{roomId}/leave")
    @Override
    public ApiResponse<Void> leaveChatRoom(
            @PathVariable("roomId") Long roomId, @AuthenticationPrincipal UserDetails userDetails) {
        String email = userDetails.getUsername();
        User user = userService.findUser(email);
        chatRoomService.leaveChatRoom(roomId, user.getId());
        return ApiResponse.of(SuccessStatus._CHATROOM_LEFT);
    }

    @GetMapping("/chats/{roomId}")
    @Override
    public ApiResponse<ChatRoomResultResponse> getChatRoomDetail(
            @PathVariable("roomId") Long roomId, @AuthenticationPrincipal UserDetails userDetails) {
        String email = userDetails.getUsername();
        User user = userService.findUser(email);

        ChatRoomResultResponse responseDTO = chatRoomQueryService.getChatRoomDetail(roomId, user);

        return ApiResponse.of(SuccessStatus._CHATROOM_FOUND, responseDTO);
    }

    @MessageMapping("/rooms/{roomId}/enter")
    public void enter(@DestinationVariable Long roomId, Principal p, StompHeaderAccessor acc) {
        Long userId = Long.parseLong(p.getName());
        presenceService.enter(roomId, userId, acc.getSessionId());

        chatNotificationService.markAsReadOnEnter(roomId, userId);
    }

    @MessageMapping("/rooms/{roomId}/ping")
    public void pingOne(@DestinationVariable Long roomId, Principal p, StompHeaderAccessor acc) {
        Long userId = Long.parseLong(p.getName());
        presenceService.touchTTL(acc.getSessionId(), roomId, userId);
    }

    @MessageMapping("/rooms/{roomId}/leave")
    public void leave(@DestinationVariable Long roomId, Principal p, StompHeaderAccessor acc) {
        Long userId = Long.parseLong(p.getName());
        presenceService.leave(roomId, userId, acc.getSessionId());
    }
}
