package com.fmi.domain.noticecomment.web.controller;

import com.fmi.domain.noticecomment.response.NoticeCommentResponse;
import com.fmi.domain.noticecomment.response.NoticeCommentSliceResponse;
import com.fmi.domain.noticecomment.service.NoticeCommentService;
import com.fmi.domain.noticecomment.web.dto.CreateNoticeCommentDto;
import com.fmi.global.apiPayload.ApiResponse;
import com.fmi.global.apiPayload.code.status.ErrorStatus;
import com.fmi.global.apiPayload.exception.GeneralException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/notices")
@RequiredArgsConstructor
@Tag(name = "NoticeComment", description = "공지사항 댓글 API")
public class NoticeCommentController {

    private final NoticeCommentService noticeCommentService;

    @PostMapping("/{noticeId}/comments")
    @Operation(summary = "공지사항 댓글 생성", description = "인증된 사용자(관리자 포함)만 작성할 수 있습니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "댓글 생성 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = "{\"isSuccess\": true, \"code\": \"COMMON200\", \"message\": \"성공\", \"result\": {\"id\": 12, \"content\": \"댓글 내용\", \"authorId\": 34, \"authorName\": \"닉네임\", \"createdAt\": \"2024-01-01T00:00:00\", \"parentId\": null, \"canEdit\": true, \"canDelete\": true}}"
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "NOTICE404-NOT_FOUND: 존재하지 않는 공지사항입니다",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = "{\"isSuccess\": false, \"code\": \"NOTICE404-NOT_FOUND\", \"message\": \"존재하지 않는 공지사항입니다.\"}"
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "COMMENT404-NOT_FOUND: 존재하지 않는 댓글입니다",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = "{\"isSuccess\": false, \"code\": \"COMMENT404-NOT_FOUND\", \"message\": \"존재하지 않는 댓글입니다.\"}"
                            )
                    )
            )
    })
    public ResponseEntity<ApiResponse<NoticeCommentResponse>> createComment(
            @PathVariable Long noticeId,
            @Valid @RequestBody CreateNoticeCommentDto request,
            @AuthenticationPrincipal UserDetails userDetails) {

        NoticeCommentResponse response = noticeCommentService.createComment(noticeId, request, userDetails);
        return ResponseEntity.ok(ApiResponse.onSuccess(response));
    }

    @GetMapping("/{noticeId}/comments")
    @Operation(summary = "공지사항 댓글 조회 (커서 기반 무한스크롤)")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "댓글 조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = "{\"isSuccess\": true, \"code\": \"COMMON200\", \"message\": \"성공\", \"result\": {\"comments\": [{\"id\": 12, \"content\": \"댓글 내용\", \"authorId\": 34, \"authorName\": \"닉네임\", \"createdAt\": \"2024-01-01T00:00:00\", \"parentId\": null, \"canEdit\": true, \"canDelete\": true}], \"hasNext\": true, \"cursor\": 12}}"
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "NOTICE404-NOT_FOUND: 존재하지 않는 공지사항입니다",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = "{\"isSuccess\": false, \"code\": \"NOTICE404-NOT_FOUND\", \"message\": \"존재하지 않는 공지사항입니다.\"}"
                            )
                    )
            )
    })
    public ResponseEntity<ApiResponse<NoticeCommentSliceResponse>> getComments(
            @PathVariable Long noticeId,
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal UserDetails userDetails) {

        NoticeCommentSliceResponse response = noticeCommentService.getComments(noticeId, cursor, size, userDetails);
        return ResponseEntity.ok(ApiResponse.onSuccess(response));
    }

    @PutMapping("/comments/{commentId}")
    @Operation(summary = "공지사항 댓글 수정", description = "작성자만 수정할 수 있으며, 관리자도 본인이 작성한 댓글만 수정 가능합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "댓글 수정 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = "{\"isSuccess\": true, \"code\": \"COMMON200\", \"message\": \"성공\", \"result\": {\"id\": 12, \"content\": \"수정된 댓글\", \"authorId\": 34, \"authorName\": \"닉네임\", \"createdAt\": \"2024-01-01T00:00:00\", \"parentId\": null, \"canEdit\": true, \"canDelete\": true}}"
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "COMMENT404-NOT_FOUND: 존재하지 않는 댓글입니다",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = "{\"isSuccess\": false, \"code\": \"COMMENT404-NOT_FOUND\", \"message\": \"존재하지 않는 댓글입니다.\"}"
                            )
                    )
            )
    })
    public ResponseEntity<ApiResponse<NoticeCommentResponse>> updateComment(
            @PathVariable Long commentId,
            @Valid @RequestBody CreateNoticeCommentDto request,
            @AuthenticationPrincipal UserDetails userDetails) {

        NoticeCommentResponse response = noticeCommentService.updateComment(commentId, request, userDetails);
        return ResponseEntity.ok(ApiResponse.onSuccess(response));
    }

    @PostMapping("/comments/{commentId}/like")
    @Operation(summary = "공지사항 댓글 추천 토글", description = "이미 추천한 경우 취소, 아닌 경우 추천합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "댓글 추천 토글 성공")
    })
    public ResponseEntity<ApiResponse<Boolean>> toggleCommentLike(
            @PathVariable Long commentId,
            @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            throw new GeneralException(ErrorStatus._UNAUTHORIZED);
        }
        boolean liked = noticeCommentService.toggleCommentLike(commentId, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.onSuccess(liked));
    }

    @DeleteMapping("/comments/{commentId}")
    @Operation(summary = "공지사항 댓글 삭제", description = "작성자 또는 관리자(ROLE_ADMIN)만 삭제할 수 있습니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "댓글 삭제 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = "{\"isSuccess\": true, \"code\": \"COMMON200\", \"message\": \"성공\", \"result\": {\"id\": 12, \"content\": \"삭제된 댓글\", \"authorId\": 34, \"authorName\": \"닉네임\", \"createdAt\": \"2024-01-01T00:00:00\", \"parentId\": null, \"canEdit\": false, \"canDelete\": true}}"
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "COMMENT404-NOT_FOUND: 존재하지 않는 댓글입니다",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = "{\"isSuccess\": false, \"code\": \"COMMENT404-NOT_FOUND\", \"message\": \"존재하지 않는 댓글입니다.\"}"
                            )
                    )
            )
    })
    public ResponseEntity<ApiResponse<NoticeCommentResponse>> deleteComment(
            @PathVariable Long commentId,
            @AuthenticationPrincipal UserDetails userDetails) {

        NoticeCommentResponse response = noticeCommentService.deleteComment(commentId, userDetails);
        return ResponseEntity.ok(ApiResponse.onSuccess(response));
    }
}
