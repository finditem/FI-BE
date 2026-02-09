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
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
                                                  "id": 34,
                                                  "nickname": "닉네임",
                                                  "profileImageUrl": "https://example.com/profile.png"
                                                },
                                                "commentImageResponseList": [
                                                  {
                                                    "id": 1,
                                                    "imageUrl": "https://example.com/comment-image.png"
                                                  }
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
    public ResponseEntity<ApiResponse<CommentCreateResponse>> createComment(@RequestBody CommentCreateRequest request,
                                                                            @RequestPart(value = "image", required = false) List<MultipartFile> images,
                                                                            @AuthenticationPrincipal UserDetails userDetails,
                                                                            @PathVariable Long postId) {

        CommentCreateResponse response = commentService.createCommentByPost(request, userDetails, postId, images);

        return ResponseEntity.ok(ApiResponse.onSuccess(response));
    }

    @GetMapping("/posts/{postId}")
    @Operation(
            summary = "게시글 댓글 조회 (커서 기반 무한스크롤)",
            description = """
                    게시글에 달린 최상위 댓글(depth = 0)을 커서 기반 무한스크롤 방식으로 조회합니다.
                    
                    - 첫 요청은 cursor 없이 호출
                    - 이후 응답의 cursor 값을 다음 요청의 cursor로 전달
                    - 대댓글은 별도 API로 조회
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "댓글 조회 성공",
                    content = @Content(
                            mediaType = "application/json",
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
                                                    "deleted": false,
                                                    "depth": 0,
                                                    "createdAt": "2024-01-01T00:00:00",
                                                    "authorResponse": {
                                                      "id": 34,
                                                      "nickname": "닉네임",
                                                      "profileImageUrl": "https://example.com/profile.png"
                                                    },
                                                    "replyCount": 3,
                                                    "nextReplyCursor": null,
                                                    "imageList": [
                                                      {
                                                        "id": 1,
                                                        "imageUrl": "https://example.com/comment-image.png"
                                                      }
                                                    ],
                                                    "childrenCommentList": [],
                                                    "likeCount": 5,
                                                    "isLike": true
                                                  }
                                                ],
                                                "hasNext": true,
                                                "cursor": 12
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
                                                                        @RequestParam(required = false) Long cursor,
                                                                        @RequestParam(defaultValue = "10") int size,
                                                                        @AuthenticationPrincipal UserDetails userDetails) {

        CommentPageResponse response = commentQueryService.getParentComments(postId, cursor, size, userDetails);

        return ResponseEntity.ok(ApiResponse.onSuccess(response));
    }

    @GetMapping("/{commentId}/replies")
    @Operation(
            summary = "댓글 대댓글 조회 (커서 기반 무한스크롤)",
            description = """
                    특정 댓글(commentId)의 대댓글 목록을 조회합니다.
                    
                    - 커서 기반 무한스크롤: 첫 요청은 cursor 없이 호출하고, 이후 응답의 cursor(nextCursor)를 cursor로 넣어 다음 페이지를 요청합니다.
                    - size: 한 번에 가져올 개수 (기본 20)
                    
                    예)
                    - 첫 요청: /comments/{commentId}/replies?size=20
                    - 다음 요청: /comments/{commentId}/replies?cursor=98&size=20
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "대댓글 조회 성공",
                    content = @Content(
                            mediaType = "application/json",
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
                                                    "deleted": false,
                                                    "depth": 1,
                                                    "createdAt": "2024-01-01T00:00:00",
                                                    "authorResponse": {
                                                      "id": 34,
                                                      "nickname": "닉네임",
                                                      "profileImageUrl": "https://example.com/profile.png"
                                                    },
                                                    "replyCount": 0,
                                                    "nextReplyCursor": null,
                                                    "imageList": [],
                                                    "childrenCommentList": [],
                                                    "likeCount": 2,
                                                    "isLike": true
                                                  }
                                                ],
                                                "hasNext": true,
                                                "cursor": 45
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
                                                                       @RequestParam(required = false) Long cursor,
                                                                       @RequestParam(defaultValue = "20") int size,
                                                                       @AuthenticationPrincipal UserDetails userDetails
    ) {
        CommentPageResponse response = commentQueryService.getReplies(commentId, cursor, size, userDetails);
        return ResponseEntity.ok(ApiResponse.onSuccess(response));
    }

    @PutMapping(value = "/{commentId}")
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
                                                  {
                                                    "id": 1,
                                                    "imageUrl": "https://example.com/comment/1.png"
                                                  }
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
    public ResponseEntity<ApiResponse<CommentCreateResponse>> updateComment(@RequestBody CommentUpdateRequest request,
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
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "COMMENT404-NOT_FOUND: 존재하지 댓글입니다.", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "COMMENT400-ALREADY_DELETED: 이미 삭제된 댓글입니다.", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "COMMON500: 서버 에러", content = @Content)
    })
    public ResponseEntity<ApiResponse<CommentDeleteResponse>> deleteComment(@AuthenticationPrincipal UserDetails userDetails,
                                                                            @PathVariable Long commentId) {

        CommentDeleteResponse response = commentService.deleteComment(commentId, userDetails);

        return ResponseEntity.ok(ApiResponse.onSuccess(response));
    }
}
