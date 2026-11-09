package com.fmi.domain.chat.room.web.swagger;

import com.fmi.domain.chat.room.web.dto.response.ChatRoomResponse.ChatRoomResultResponse;
import com.fmi.domain.chat.room.web.dto.response.ChatRoomResponse.MyChatListResponse;
import com.fmi.domain.post.post.data.enums.PostType;
import com.fmi.global.apiPayload.ApiResponse;
import com.fmi.global.enums.SortType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;

@Tag(name = "ChatRoom", description = "채팅방 관련 API")
public interface ChatRoomSwagger {

    @Operation(summary = "채팅방 생성/조회", description = "특정 게시글에 대해 1:1 채팅방을 생성하거나 기존 채팅방을 조회합니다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "201",
                description = "CHATROOM_CREATED: 채팅방이 생성되었습니다."),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "CHATROOM_FOUND: 기존 채팅방을 조회합니다."),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "403",
                description = "CHATROOM_NOT_ALLOWED: 자신의 글에는 채팅할 수 없습니다."),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "POST_NOT_FOUND: 존재하지 않는 게시글입니다.")
    })
    ResponseEntity<ApiResponse<ChatRoomResultResponse>> createChatRoom(
            Long postId, @Parameter(hidden = true) UserDetails userDetails);

    @Operation(summary = "내 채팅 목록 조회", description = "내가 참여하고 있는 채팅방 목록을 커서 기반 페이지네이션으로 조회합니다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "CHATROOM_LIST_FETCHED: 나의 채팅 목록 조회에 성공했습니다.")
    })
    @Parameters({
        @Parameter(name = "cursor", description = "이전 페이지 응답의 nextCursor 값. 첫 페이지 조회 시 생략.", required = false),
        @Parameter(name = "size", description = "한 페이지에 조회할 개수. (기본값: 10)", required = false),
        @Parameter(name = "type", description = "습득물/분실물(FOUND/LOST) 타입", required = false),
        @Parameter(name = "address", description = "지역(ex.서울시 동작구) 필터", required = false),
        @Parameter(name = "sort", description = "최신순/오래된순(LATEST/OLDEST) 정렬. 기본값 : 최신순(LATEST)", required = false)
    })
    ApiResponse<MyChatListResponse> getMyPostChatRooms(
            Long cursor,
            int size,
            PostType type,
            String address,
            SortType sort,
            @Parameter(hidden = true) UserDetails userDetails);

    @Operation(
            summary = "채팅방 나가기",
            description =
                    "채팅방을 나가면 그 시점부터 나간 사용자에게 대화 내역이 보이지 않고, 내 채팅 목록에도 사라집니다. 다시 채팅이 시작되면 그 나간 이후 시점부터 내역이 보입니다. (나가지 않은 사용자에겐 적용x)")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "CHATROOM-LEFT: 채팅방 나가기를 성공했습니다."),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "CHATROOM-PARTICIPANT_NOT_FOUND: 참여 중인 채팅방이 아닙니다.")
    })
    ApiResponse<Void> leaveChatRoom(Long roomId, @Parameter(hidden = true) UserDetails userDetails);

    @Operation(summary = "채팅방 상세 조회", description = "특정 채팅방의 상세 정보(상대방 정보, 게시글 요약 등)를 조회합니다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "CHATROOM_FOUND: 기존 채팅방을 조회합니다."),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "403",
                description = "CHATROOM_ACCESS_DENIED: 해당 채팅방에 접근 권한이 없습니다."),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "CHATROOM_NOT_FOUND: 존재하지 않는 채팅방입니다.")
    })
    ApiResponse<ChatRoomResultResponse> getChatRoomDetail(
            Long roomId, @Parameter(hidden = true) UserDetails userDetails);
}
