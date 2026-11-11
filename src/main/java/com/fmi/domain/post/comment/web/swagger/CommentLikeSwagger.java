package com.fmi.domain.post.comment.web.swagger;

import com.fmi.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;

@Tag(name = "Comment", description = "댓글 관련 API")
public interface CommentLikeSwagger {

    @Operation(summary = "댓글 좋아요 생성", description = "댓글에 좋아요를 누릅니다. 이미 좋아요 데이터가 있으면 활성화 처리합니다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "좋아요 성공",
                content =
                        @Content(
                                mediaType = "application/json",
                                examples =
                                        @ExampleObject(
                                                value =
                                                        "{\"isSuccess\": true, \"code\": \"COMMON200\", \"message\": \"성공\", \"result\": true}"))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "401",
                description = "COMMON401: 인증이 필요합니다",
                content = @Content),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "COMMENT404-NOT_FOUND: 존재하지 않는 댓글입니다.",
                content = @Content),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "COMMENT400-ALREADY_DELETED: 이미 삭제된 댓글입니다.",
                content = @Content),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "500",
                description = "COMMON500: 서버 에러",
                content = @Content)
    })
    ResponseEntity<ApiResponse<Boolean>> createLike(Long commentId, @Parameter(hidden = true) UserDetails userDetails);

    @Operation(summary = "댓글 좋아요 취소", description = "댓글 좋아요를 취소합니다. 좋아요 정보가 없으면 에러를 반환합니다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "좋아요 취소 성공",
                content =
                        @Content(
                                mediaType = "application/json",
                                examples =
                                        @ExampleObject(
                                                value =
                                                        "{\"isSuccess\": true, \"code\": \"COMMON200\", \"message\": \"성공\", \"result\": false}"))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "401",
                description = "COMMON401: 인증이 필요합니다",
                content = @Content),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = """
                        - COMMENT404-NOT_FOUND: 존재하지 않는 댓글입니다.
                        - COMMENT404-LIKE_NOT_FOUND: 존재하지 않는 댓글 좋아요입니다.
                        """,
                content = @Content),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "COMMENT400-ALREADY_DELETED: 이미 삭제된 댓글입니다.",
                content = @Content),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "500",
                description = "COMMON500: 서버 에러",
                content = @Content)
    })
    ResponseEntity<ApiResponse<Boolean>> deleteLike(Long commentId, @Parameter(hidden = true) UserDetails userDetails);
}
