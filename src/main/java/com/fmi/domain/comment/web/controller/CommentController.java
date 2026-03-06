package com.fmi.domain.comment.web.controller;

import com.fmi.domain.comment.service.CommentQueryService;
import com.fmi.domain.comment.service.CommentService;
import com.fmi.domain.comment.web.dto.request.CommentCreateRequest;
import com.fmi.domain.comment.web.dto.request.CommentUpdateRequest;
import com.fmi.domain.comment.web.dto.response.CommentCreateResponse;
import com.fmi.domain.comment.web.dto.response.CommentDeleteResponse;
import com.fmi.domain.comment.web.dto.response.CommentPageResponse;
import com.fmi.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RequestMapping("/comments")
@RestController
@RequiredArgsConstructor
@Tag(name = "Comment", description = "댓글 관련 API")
public class CommentController {

    private final CommentService commentService;
    private final CommentQueryService commentQueryService;

    @PostMapping(value = "/posts/{postId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "댓글 생성",
            description = """
                    게시글에 댓글/대댓글을 생성합니다.
                    
                    - parentId가 null이면 댓글(depth=0)
                    - parentId가 있으면 대댓글(depth=1~2)
                    - 이미지 첨부 가능 (multipart/form-data)
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "댓글 생성 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = CommentCreateResponse.class),
                            examples = @ExampleObject(
                                    value = """
                                            {
                                              "isSuccess": true,
                                              "code": "COMMON200",
                                              "message": "성공",
                                              "result": {
                                                "id": 12,
                                                "content": "댓글 내용",
                                                "createdAt": "2024-01-01T00:00:00",
                                                "likeCount": 0,
                                                "canEdit": true,
                                                "canDelete": true,
                                                "authorResponse": {
                                                  "userId": 34,
                                                  "nickName": "닉네임",
                                                  "profileImage": "https://example.com/profile.png"
                                                },
                                                "commentImageResponseList": [
                                                  { "id": 1, "imageUrl": "https://example.com/comment-image.png" },
                                                  { "id": 2, "imageUrl": "https://example.com/comment-image-2.png" }
                                                ]
                                              }
                                            }
                                            """
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "COMMON400: 잘못된 요청입니다", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "COMMON401: 인증이 필요합니다", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "POST404-NOT_FOUND: 존재하지 않는 게시글입니다", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "COMMENT404_PARENT-NOT_FOUND: 존재하지 않는 부모 댓글입니다.", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "COMMENT400_DEPTH-EXCEEDED: 대댓글은 3단계까지만 작성할 수 있습니다.", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "COMMON500: 서버 에러", content = @Content)
    })
    public ResponseEntity<ApiResponse<CommentCreateResponse>> createComment(@RequestPart("request") CommentCreateRequest request,
                                                                            @RequestPart(value = "image", required = false) List<MultipartFile> images,
                                                                            @AuthenticationPrincipal UserDetails userDetails,
                                                                            @PathVariable Long postId) {

        CommentCreateResponse response = commentService.createCommentByPost(request, userDetails, postId, images);

        return ResponseEntity.ok(ApiResponse.onSuccess(response));
    }

    @GetMapping("/posts/{postId}")
    @Operation(
            summary = "게시글 댓글 조회 (페이지네이션 - 더보기)",
            description = """
                    게시글에 달린 최상위 댓글(depth = 0)을 페이지네이션 방식으로 조회합니다.
                    
                    - size는 서버에서 10개로 고정됩니다.
                    - 첫 요청은 page=0
                    - '댓글 더보기' 클릭 시 nextPage 값을 page로 넣어 다음 페이지를 요청합니다.
                    - 차단한 유저/나를 차단한 유저의 댓글은 제외됩니다.
                    - 대댓글은 별도 API로 조회합니다.
                    
                    예)
                    - 첫 요청: /comments/posts/{postId}?page=0
                    - 다음 요청: /comments/posts/{postId}?page=1
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "댓글 조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = CommentPageResponse.class),
                            examples = @ExampleObject(
                                    value = """
                                            {
                                              "isSuccess": true,
                                              "code": "COMMON200",
                                              "message": "성공",
                                              "result": {
                                                "comments": [
                                                  {
                                                    "id": 12,
                                                    "content": "댓글 내용입니다.",
                                                    "deleted": false,
                                                    "depth": 0,
                                                    "createdAt": "2024-01-01T00:00:00",
                                                    "authorResponse": {
                                                      "id": 34,
                                                      "nickname": "닉네임",
                                                      "profileImageUrl": "https://example.com/profile.png"
                                                    },
                                                    "childCommentCount": 3,
                                                    "imageList": [
                                                      {
                                                        "id": 1,
                                                        "imageUrl": "https://example.com/comment-image.png"
                                                      }
                                                    ],
                                                    "likeCount": 5,
                                                    "isLike": true,
                                                    "canEdit": true,
                                                    "canDelete": true
                                                  }
                                                ],
                                                "hasNext": true,
                                                "nextPage": 1,
                                                "remainingCount": 27
                                              }
                                            }
                                            """
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "LIST400-INVALID_CURSOR: 유효하지 않은 커서입니다",
                    content = @Content
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "POST404-NOT_FOUND: 존재하지 않는 게시글입니다",
                    content = @Content
            )
    })
    public ResponseEntity<ApiResponse<CommentPageResponse>> getComments(@PathVariable Long postId,
                                                                        @RequestParam(defaultValue = "0") int page,
                                                                        @AuthenticationPrincipal UserDetails userDetails) {

        CommentPageResponse response = commentQueryService.getParentComments(postId, page, userDetails);

        return ResponseEntity.ok(ApiResponse.onSuccess(response));
    }

    @GetMapping("/{commentId}/replies")
    @Operation(
            summary = "댓글 대댓글 조회 (페이지네이션 - 더보기)",
            description = """
                    특정 댓글(commentId)의 대댓글 목록을 페이지네이션 방식으로 조회합니다.
                    
                    - size는 서버에서 10개로 고정됩니다.
                    - 첫 요청은 page=0
                    - '대댓글 더보기' 클릭 시 nextPage 값을 page로 넣어 다음 페이지를 요청합니다.
                    - 차단한 유저/나를 차단한 유저의 댓글은 제외됩니다.
                    
                    예)
                    - 첫 요청: /comments/{commentId}/replies?page=0
                    - 다음 요청: /comments/{commentId}/replies?page=1
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "대댓글 조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = CommentPageResponse.class),
                            examples = @ExampleObject(
                                    value = """
                                            {
                                              "isSuccess": true,
                                              "code": "COMMON200",
                                              "message": "성공",
                                              "result": {
                                                "comments": [
                                                  {
                                                    "id": 45,
                                                    "content": "대댓글 내용입니다.",
                                                    "deleted": false,
                                                    "depth": 1,
                                                    "createdAt": "2024-01-01T00:00:00",
                                                    "authorResponse": {
                                                      "id": 34,
                                                      "nickname": "닉네임",
                                                      "profileImageUrl": "https://example.com/profile.png"
                                                    },
                                                    "childCommentCount": 0,
                                                    "imageList": [],
                                                    "likeCount": 2,
                                                    "isLike": true,
                                                    "canEdit": true,
                                                    "canDelete": true
                                                  }
                                                ],
                                                "hasNext": true,
                                                "nextPage": 1,
                                                "remainingCount": 8
                                              }
                                            }
                                            """
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "LIST400-INVALID_CURSOR: 유효하지 않은 커서입니다", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "COMMENT404_PARENT-NOT_FOUND: 존재하지 않는 부모 댓글입니다."),
    })
    public ResponseEntity<ApiResponse<CommentPageResponse>> getReplies(@PathVariable Long commentId,
                                                                       @RequestParam(defaultValue = "0") int page,
                                                                       @AuthenticationPrincipal UserDetails userDetails
    ) {
        CommentPageResponse response = commentQueryService.getReplies(commentId, page, userDetails);
        return ResponseEntity.ok(ApiResponse.onSuccess(response));
    }

    @PutMapping(value = "/{commentId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "댓글 수정",
            description = """
                    작성자만 댓글을 수정할 수 있습니다.
                    
                    - 삭제된 댓글은 수정할 수 없습니다.
                    - 이미지 추가/삭제를 지원합니다.
                      - image: 추가할 이미지 파일 리스트 (optional)
                      - deleteImageIds: 삭제할 이미지 ID 리스트 (optional)
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "댓글 수정 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = CommentCreateResponse.class),
                            examples = @ExampleObject(
                                    value = """
                                            {
                                              "isSuccess": true,
                                              "code": "COMMON200",
                                              "message": "성공",
                                              "result": {
                                                "id": 12,
                                                "content": "수정된 댓글",
                                                "createdAt": "2024-01-01T00:00:00",
                                                "likeCount": 0,
                                                "canEdit": true,
                                                "canDelete": true,
                                                "authorResponse": {
                                                  "userId": 34,
                                                  "nickName": "닉네임",
                                                  "profileImage": "https://example.com/profile.png"
                                                },
                                                "commentImageResponseList": [
                                                  { "id": 1, "imageUrl": "https://example.com/comment/1.png" }
                                                ]
                                              }
                                            }
                                            """
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "COMMON400: 잘못된 요청입니다", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "COMMENT400-ALREADY_DELETED: 이미 삭제된 댓글입니다.", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "COMMENT400-IMAGE_NOT_OWNED: 해당 댓글에 속하지 않는 이미지입니다.", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "COMMON401: 인증이 필요합니다", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "COMMENT403-ACCESS_DENIED: 댓글에 접근 권한이 없습니다", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "COMMENT404-NOT_FOUND: 존재하지 않는 댓글입니다", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "COMMON500: 서버 에러", content = @Content)
    })
    public ResponseEntity<ApiResponse<CommentCreateResponse>> updateComment(@RequestPart("request") @Valid CommentUpdateRequest request,
                                                                            @AuthenticationPrincipal UserDetails userDetails,
                                                                            @PathVariable Long commentId,
                                                                            @RequestPart(value = "image", required = false) List<MultipartFile> images) {

        CommentCreateResponse response = commentService.updateComment(request, userDetails, commentId, images);

        return ResponseEntity.ok(ApiResponse.onSuccess(response));
    }

    @DeleteMapping("/{commentId}")
    @Operation(
            summary = "댓글 삭제",
            description = """
                    댓글을 삭제합니다. (Soft Delete)
                    
                    - 작성자만 삭제할 수 있습니다.
                    - 삭제된 댓글은 deleted=true 처리되며, content는 '삭제된 댓글입니다.'로 변경됩니다.
                    - 댓글 이미지/좋아요는 함께 정리됩니다.
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "댓글 삭제 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = CommentDeleteResponse.class),
                            examples = @ExampleObject(
                                    value = """
                                            {
                                              "isSuccess": true,
                                              "code": "COMMON200",
                                              "message": "성공",
                                              "result": {
                                                "id": 12,
                                                "content": "삭제된 댓글입니다."
                                              }
                                            }
                                            """
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "COMMON401: 인증이 필요합니다", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "COMMENT403-ACCESS_DENIED: 댓글에 접근 권한이 없습니다", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "COMMENT404-NOT_FOUND: 존재하지 않는 댓글입니다.", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "COMMENT400-ALREADY_DELETED: 이미 삭제된 댓글입니다.", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "COMMON500: 서버 에러", content = @Content)
    })
    public ResponseEntity<ApiResponse<CommentDeleteResponse>> deleteComment(@AuthenticationPrincipal UserDetails userDetails,
                                                                            @PathVariable Long commentId) {

        CommentDeleteResponse response = commentService.deleteComment(commentId, userDetails);

        return ResponseEntity.ok(ApiResponse.onSuccess(response));
    }
}
