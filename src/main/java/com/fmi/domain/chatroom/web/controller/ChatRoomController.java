package com.fmi.domain.chatroom.web.controller;

import com.fmi.domain.Enum.SortType;
import com.fmi.domain.Enum.Type;
import com.fmi.domain.auth.data.User;
import com.fmi.domain.chatroom.service.ChatRoomPresenceService;
import com.fmi.domain.chatroom.service.ChatRoomService;
import com.fmi.global.apiPayload.ApiResponse;
import com.fmi.global.apiPayload.code.status.SuccessStatus;
import com.fmi.service.UserQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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

import java.security.Principal;

import static com.fmi.domain.chatroom.web.dto.ChatRoomResponseDTO.ChatRoomResultDTO;
import static com.fmi.domain.chatroom.web.dto.ChatRoomResponseDTO.MyChatListDTO;

@RestController
@RequiredArgsConstructor
public class ChatRoomController {

    private final UserQueryService userService;
    private final ChatRoomService chatRoomService;
    private final ChatRoomPresenceService presenceService;

    @Operation(summary = "채팅방 생성/조회", description = "특정 게시글에 대해 1:1 채팅방을 생성하거나 기존 채팅방을 조회합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "CHATROOM_CREATED: 채팅방이 새로 생성됨"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "CHATROOM_FOUND: 기존 채팅방이 조회됨"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "CHATROOM_NOT_ALLOWED: 자신의 게시글에 채팅 시도"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "POST_NOT_FOUND: 존재하지 않는 게시글"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "USER_NOT_FOUND: 존재하지 않는 사용자")
    })
    @PostMapping("/posts/{postId}/chats")
    public ResponseEntity<ApiResponse<ChatRoomResultDTO>> createChatRoom(@PathVariable("postId") Long postId, @AuthenticationPrincipal UserDetails userDetails) {
        String email = userDetails.getUsername();
        User user = userService.findUser(email);
        Pair<ChatRoomResultDTO, Boolean> result = chatRoomService.createChatRoom(postId, user.getId());

        ChatRoomResultDTO responseDTO = result.getFirst();
        boolean isNew = result.getSecond();

        if (isNew) {
            // 새로 생성된 경우
            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(ApiResponse.of(SuccessStatus._CHATROOM_CREATED, responseDTO));
        } else {
            // 기존에 존재했던 경우
            return ResponseEntity
                    .status(HttpStatus.OK)
                    .body(ApiResponse.of(SuccessStatus._CHATROOM_FOUND, responseDTO));
        }
    }

    @Operation(summary = "내 채팅 목록 조회", description = "내가 참여하고 있는 채팅방 목록을 커서 기반 페이지네이션으로 조회합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "CHATROOM_LIST_FETCHED: 채팅 목록 조회 성공")
    })
    @Parameters({
            @Parameter(name = "cursor", description = "이전 페이지 응답의 nextCursor 값. 첫 페이지 조회 시 생략.", required = false),
            @Parameter(name = "size", description = "한 페이지에 조회할 개수. (기본값: 10)", required = false),
            @Parameter(name = "type", description = "습득물/분실물(FOUND/LOST) 타입", required = false),
            @Parameter(name = "address", description = "지역(ex.서울시 동작구) 필터", required = false),
            @Parameter(name = "sort", description = "최신순/오래된순(LATEST/OLDEST) 정렬. 기본값 : 최신순(LATEST)", required = false),

    })
    @GetMapping("/users/me/chats")
    public ApiResponse<MyChatListDTO> getMyPostChatRooms(
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(name = "type", required = false) Type type,
            @RequestParam(name = "address", required = false) String address,
            @RequestParam(name= "sort", required = false, defaultValue = "LATEST") SortType sort,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        User user = userService.findUser(userDetails.getUsername());

        MyChatListDTO responseDTO = chatRoomService.getMyPostChatRooms(user, cursor, size, type, address, sort);
        return ApiResponse.of(SuccessStatus._CHATROOM_LIST_FETCHED, responseDTO);
    }

    @Operation(summary = "채팅방 나가기", description = "채팅방을 나가면 그 시점부터 나간 사용자에게 대화 내역이 보이지 않고, 내 채팅 목록에도 사라집니다. 다시 채팅이 시작되면 그 나간 이후 시점부터 내역이 보입니다. (나가지 않은 사용자에겐 적용x)")
    @PostMapping("/chats/{roomId}/leave")
    public ApiResponse<Void> leaveChatRoom(@PathVariable("roomId") Long roomId, @AuthenticationPrincipal UserDetails userDetails) {
        String email = userDetails.getUsername();
        User user = userService.findUser(email);
        chatRoomService.leaveChatRoom(roomId, user.getId());
        return ApiResponse.of(SuccessStatus._CHATROOM_LEFT);
    }

    @MessageMapping("/rooms/{roomId}/enter")
    public void enter(@DestinationVariable Long roomId, Principal p, StompHeaderAccessor acc) {
        Long userId = Long.parseLong(p.getName());
        presenceService.enter(roomId, userId, acc.getSessionId());
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
