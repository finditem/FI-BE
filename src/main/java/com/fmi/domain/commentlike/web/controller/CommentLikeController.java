package com.fmi.domain.commentlike.web.controller;

import com.fmi.domain.commentlike.service.CommentLikeService;
import com.fmi.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Tag(name = "Comment", description = "댓글 관련 API")
public class CommentLikeController {

    private final CommentLikeService commentLikeService;

    @PostMapping("/comments/{commentId}/likes")
    @Operation(summary = "댓글 좋아요 생성", description = "댓글에 좋아요를 누릅니다. 이미 좋아요 데이터가 있으면 활성화 처리합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "좋아요 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = "{\"isSuccess\": true, \"code\": \"COMMON200\", \"message\": \"성공\", \"result\": true}"
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "COMMON401: 인증이 필요합니다", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "COMMENT404-NOT_FOUND: 존재하지 댓글입니다.", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "COMMENT400-ALREADY_DELETED: 이미 삭제된 댓글입니다.", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "COMMON500: 서버 에러", content = @Content)
    })
    public ResponseEntity<ApiResponse<Boolean>> createLike(@PathVariable Long commentId,
                                                           @AuthenticationPrincipal UserDetails userDetails) {
        commentLikeService.createLike(commentId, userDetails);

        return ResponseEntity.ok(ApiResponse.onSuccess(true));
    }

    @DeleteMapping("/comments/{commentId}/likes")
    @Operation(summary = "댓글 좋아요 취소", description = "댓글 좋아요를 취소합니다. 좋아요 정보가 없으면 에러를 반환합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "좋아요 취소 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = "{\"isSuccess\": true, \"code\": \"COMMON200\", \"message\": \"성공\", \"result\": false}"
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "COMMON401: 인증이 필요합니다", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "COMMENT404-NOT_FOUND: 존재하지 댓글입니다.", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "COMMENT400-ALREADY_DELETED: 이미 삭제된 댓글입니다.", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "COMMENT404-LIKE_NOT_FOUND: 존재하지 않는 댓글 좋아요입니다.", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "COMMON500: 서버 에러", content = @Content)
    })
    public ResponseEntity<ApiResponse<Boolean>> deleteLike(@PathVariable Long commentId,
                                                           @AuthenticationPrincipal UserDetails userDetails) {

        commentLikeService.deleteLike(commentId, userDetails);

        return ResponseEntity.ok(ApiResponse.onSuccess(false));
    }

}
