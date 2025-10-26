package com.fmi.domain.chatroom.web.controller;

import com.fmi.domain.auth.data.User;
import com.fmi.domain.chatroom.service.ChatRoomService;
import com.fmi.domain.chatroom.web.dto.ChatRoomResponseDTO;
import com.fmi.global.apiPayload.ApiResponse;
import com.fmi.global.apiPayload.code.status.SuccessStatus;
import com.fmi.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.data.util.Pair;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import static com.fmi.domain.chatroom.web.dto.ChatRoomResponseDTO.ChatRoomResultDTO;

@RestController
@RequiredArgsConstructor
public class ChatRoomController {

    private final UserService userService;
    private final ChatRoomService chatRoomService;

    @Operation(summary = "채팅방 생성/조회", description = "특정 게시글에 대해 1:1 채팅방을 생성하거나 기존 채팅방을 조회합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "CHATROOM_CREATED: 채팅방이 새로 생성됨"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "CHATROOM_FOUND: 기존 채팅방이 조회됨"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "CHATROOM_NOT_ALLOWED: 자신의 게시글에 채팅 시도"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "POST_NOT_FOUND: 존재하지 않는 게시글"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "USER_NOT_FOUND: 존재하지 않는 사용자")
    })
    @PostMapping("/posts/{postId}/chats")
    public ApiResponse<ChatRoomResultDTO> createChatRoom(@PathVariable("postId") Long postId, @AuthenticationPrincipal UserDetails userDetails) {
        String email = userDetails.getUsername();
        User user = userService.findUser(email);
        Pair<ChatRoomResultDTO, Boolean> result = chatRoomService.createChatRoom(postId, user.getId());

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

    @Operation(summary = "내 채팅 목록 조회", description = "내가 참여하고 있는 채팅방 목록을 커서 기반 페이지네이션으로 조회합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "CHATROOM_LIST_FETCHED: 채팅 목록 조회 성공")
    })
    @Parameters({
            @Parameter(name = "cursor", description = "이전 페이지 응답의 nextCursor 값. 첫 페이지 조회 시 생략.", required = false),
            @Parameter(name = "size", description = "한 페이지에 조회할 개수. (기본값: 10)", required = false)
    })
    @GetMapping("/users/me/chats")
    public ApiResponse<ChatRoomResponseDTO.MyChatListDTO> getMyPostChatRooms(
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        User user = userService.findUser(userDetails.getUsername());

        ChatRoomResponseDTO.MyChatListDTO responseDTO = chatRoomService.getMyPostChatRooms(user, cursor, size);
        return ApiResponse.of(SuccessStatus._CHATROOM_LIST_FETCHED, responseDTO);
    }

    @PatchMapping("/chats/{roomId}/leave")
    public ApiResponse<Void> leftChatRoom(@PathVariable("roomId") Long roomId, @AuthenticationPrincipal UserDetails userDetails) {
        String email = userDetails.getUsername();
        User user = userService.findUser(email);
        chatRoomService.leftChatRoom(roomId, user.getId());
        return ApiResponse.of(SuccessStatus._CHATROOM_LEFT);
    }

}
