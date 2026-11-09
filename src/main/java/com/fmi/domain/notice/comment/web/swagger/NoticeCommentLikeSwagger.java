package com.fmi.domain.notice.comment.web.swagger;

import com.fmi.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;

@Tag(name = "NoticeComment", description = "공지사항 댓글 API")
public interface NoticeCommentLikeSwagger {

    @Operation(summary = "공지사항 댓글 추천 추가")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "추천 추가 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "401",
                description = "COMMON401: 로그인이 필요합니다."),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "COMMENT404-NOT_FOUND: 댓글이 없거나 USER404-NOT_FOUND: 사용자가 존재하지 않습니다.")
    })
    ResponseEntity<ApiResponse<Void>> addCommentLike(Long commentId, @Parameter(hidden = true) UserDetails userDetails);

    @Operation(summary = "공지사항 댓글 추천 취소")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "추천 취소 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "401",
                description = "COMMON401: 로그인이 필요합니다."),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "COMMENT404-NOT_FOUND: 댓글이 없거나 USER404-NOT_FOUND: 사용자가 존재하지 않습니다.")
    })
    ResponseEntity<ApiResponse<Void>> removeCommentLike(
            Long commentId, @Parameter(hidden = true) UserDetails userDetails);
}
