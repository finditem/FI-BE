package com.fmi.domain.comment.web.controller;

import com.fmi.domain.comment.response.CommentResponse;
import com.fmi.domain.comment.response.CommentSliceResponse;
import com.fmi.domain.comment.service.CommentService;
import com.fmi.domain.comment.web.dto.CreateCommentDto;
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
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RequestMapping("comments")
@RestController
@RequiredArgsConstructor
@Tag(name = "Comment", description = "댓글 관련 API")
public class CommentController {

    private final CommentService commentService;

    @PostMapping(value = "/{postId}")
    @Operation(summary = "댓글 생성")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "댓글 생성 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = "{\"isSuccess\": true, \"code\": \"COMMON200\", \"message\": \"성공\", \"result\": {\"id\": 12, \"content\": \"댓글 내용\", \"authorId\": 34, \"authorName\": \"닉네임\", \"createdAt\": \"2024-01-01T00:00:00\", \"likeCount\": 0, \"parentId\": null, \"canEdit\": true, \"canDelete\": true}}"
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "COMMON400: 잘못된 요청입니다"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "COMMON401: 인증이 필요합니다"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "POST404-NOT_FOUND: 존재하지 않는 게시글입니다"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "COMMON500: 서버 에러")
    })
    public ResponseEntity<ApiResponse<CommentResponse>> createComment(
            @RequestBody CreateCommentDto request,
            @RequestPart(value = "image", required = false) List<MultipartFile> images,
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long postId) {

        CommentResponse response = commentService.createComment(request,userDetails,postId,images);

        return ResponseEntity.ok(ApiResponse.onSuccess(response));
    }

    @GetMapping("/{postId}")
    @Operation(summary = "게시글 댓글 조회 (커서 기반 무한스크롤)")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "댓글 조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = "{\"isSuccess\": true, \"code\": \"COMMON200\", \"message\": \"성공\", \"result\": {\"comments\": [{\"id\": 12, \"content\": \"댓글 내용\", \"authorId\": 34, \"authorName\": \"닉네임\", \"createdAt\": \"2024-01-01T00:00:00\", \"likeCount\": 2, \"parentId\": null, \"canEdit\": false, \"canDelete\": true}], \"hasNext\": true, \"cursor\": 12}}"
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "LIST400-INVALID_CURSOR: 유효하지 않은 커서입니다"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "POST404-NOT_FOUND: 존재하지 않는 게시글입니다"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "COMMON500: 서버 에러")
    })
    public ResponseEntity<ApiResponse<CommentSliceResponse>> getComments(
            @PathVariable Long postId,
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal UserDetails userDetails) {

        CommentSliceResponse response = commentService.getComments(postId, cursor, size, userDetails);

        return ResponseEntity.ok(ApiResponse.onSuccess(response));
    }

    @PutMapping(value = "/{commentId}")
    @Operation(summary = "댓글 수정", description = "작성자만 수정할 수 있습니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "댓글 수정 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = "{\"isSuccess\": true, \"code\": \"COMMON200\", \"message\": \"성공\", \"result\": {\"id\": 12, \"content\": \"수정된 댓글\", \"authorId\": 34, \"authorName\": \"닉네임\", \"createdAt\": \"2024-01-01T00:00:00\", \"likeCount\": 1, \"parentId\": null, \"canEdit\": true, \"canDelete\": true}}"
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "COMMON400: 잘못된 요청입니다"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "COMMON401: 인증이 필요합니다"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "COMMON403: 금지된 요청입니다"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "COMMENT404-NOT_FOUND: 존재하지 않는 댓글입니다"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "COMMON500: 서버 에러")
    })
    public ResponseEntity<ApiResponse<CommentResponse>> updateComment(
            @RequestBody CreateCommentDto request,
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long commentId) {

        CommentResponse response = commentService.updateComment(request,userDetails,commentId);

        return ResponseEntity.ok(ApiResponse.onSuccess(response));
    }

    @DeleteMapping(value = "/{commentId}")
    @Operation(summary = "댓글 삭제", description = "작성자 또는 관리자(ROLE_ADMIN)만 삭제할 수 있습니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "댓글 삭제 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = "{\"isSuccess\": true, \"code\": \"COMMON200\", \"message\": \"성공\", \"result\": {\"id\": 12, \"content\": \"삭제된 댓글\", \"authorId\": 34, \"authorName\": \"닉네임\", \"createdAt\": \"2024-01-01T00:00:00\", \"likeCount\": 1, \"parentId\": null, \"canEdit\": false, \"canDelete\": true}}"
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "COMMON401: 인증이 필요합니다"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "COMMON403: 금지된 요청입니다"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "COMMENT404-NOT_FOUND: 존재하지 않는 댓글입니다"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "COMMON500: 서버 에러")
    })
    public ResponseEntity<ApiResponse<CommentResponse>> deleteComment(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long commentId) {

        CommentResponse response = commentService.deleteComment(userDetails,commentId);

        return ResponseEntity.ok(ApiResponse.onSuccess(response));
    }
}
