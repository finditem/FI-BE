package com.fmi.domain.findcompletion.web.swagger;

import com.fmi.domain.findcompletion.web.dto.response.FindCompletionResponse;
import com.fmi.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PathVariable;

@Tag(name = "ChatRoom", description = "채팅방 관련 API")
public interface FindCompletionSwagger {

    @Operation(
            summary = "분실물 찾기 완료",
            description = "분실물 게시글 작성자가 채팅방에서 찾기 완료를 처리합니다. 채팅 상대가 도움을 준 사용자로 기록되고 게시글은 찾음 상태가 됩니다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "찾기 완료 처리 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "FIND_COMPLETION400-INVALID_POST_TYPE: 분실물 게시글이 아닙니다."),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "403",
                description = "CHATROOM403-ACCESS_DENIED: 채팅방 참여자가 아닙니다. "
                        + "FIND_COMPLETION403-ACCESS_DENIED: 게시글 작성자가 아닙니다."),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "CHATROOM404-NOT_FOUND: 존재하지 않는 채팅방입니다. POST404-NOT_FOUND: 게시글이 없거나 삭제되었습니다."),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "409",
                description = "FIND_COMPLETION409-ALREADY_COMPLETED: 이미 찾기 완료된 게시글입니다.")
    })
    ApiResponse<FindCompletionResponse> complete(
            @PathVariable Long roomId, @AuthenticationPrincipal UserDetails userDetails);
}
