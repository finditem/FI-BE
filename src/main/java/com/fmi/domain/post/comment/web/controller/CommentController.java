package com.fmi.domain.post.comment.web.controller;

import com.fmi.domain.post.comment.service.CommentQueryService;
import com.fmi.domain.post.comment.service.CommentService;
import com.fmi.domain.post.comment.web.dto.request.CommentCreateRequest;
import com.fmi.domain.post.comment.web.dto.request.CommentUpdateRequest;
import com.fmi.domain.post.comment.web.dto.response.CommentCreateResponse;
import com.fmi.domain.post.comment.web.dto.response.CommentDeleteResponse;
import com.fmi.domain.post.comment.web.dto.response.CommentPageResponse;
import com.fmi.domain.post.comment.web.swagger.CommentSwagger;
import com.fmi.global.apiPayload.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RequestMapping("/comments")
@RestController
@RequiredArgsConstructor
public class CommentController implements CommentSwagger {

    private final CommentService commentService;
    private final CommentQueryService commentQueryService;

    @PostMapping(value = "/posts/{postId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Override
    public ResponseEntity<ApiResponse<CommentCreateResponse>> createComment(
            @RequestPart("request") CommentCreateRequest request,
            @RequestPart(value = "image", required = false) List<MultipartFile> images,
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long postId) {

        CommentCreateResponse response = commentService.createCommentByPost(request, userDetails, postId, images);

        return ResponseEntity.ok(ApiResponse.onSuccess(response));
    }

    @GetMapping("/posts/{postId}")
    @Override
    public ResponseEntity<ApiResponse<CommentPageResponse>> getComments(
            @PathVariable Long postId,
            @RequestParam(defaultValue = "0") int page,
            @AuthenticationPrincipal UserDetails userDetails) {

        CommentPageResponse response = commentQueryService.getParentComments(postId, page, userDetails);

        return ResponseEntity.ok(ApiResponse.onSuccess(response));
    }

    @GetMapping("/{commentId}/replies")
    @Override
    public ResponseEntity<ApiResponse<CommentPageResponse>> getReplies(
            @PathVariable Long commentId,
            @RequestParam(defaultValue = "0") int page,
            @AuthenticationPrincipal UserDetails userDetails) {
        CommentPageResponse response = commentQueryService.getReplies(commentId, page, userDetails);
        return ResponseEntity.ok(ApiResponse.onSuccess(response));
    }

    @PutMapping(value = "/{commentId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Override
    public ResponseEntity<ApiResponse<CommentCreateResponse>> updateComment(
            @RequestPart("request") @Valid CommentUpdateRequest request,
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long commentId,
            @RequestPart(value = "image", required = false) List<MultipartFile> images) {

        CommentCreateResponse response = commentService.updateComment(request, userDetails, commentId, images);

        return ResponseEntity.ok(ApiResponse.onSuccess(response));
    }

    @DeleteMapping("/{commentId}")
    @Override
    public ResponseEntity<ApiResponse<CommentDeleteResponse>> deleteComment(
            @AuthenticationPrincipal UserDetails userDetails, @PathVariable Long commentId) {

        CommentDeleteResponse response = commentService.deleteComment(commentId, userDetails);

        return ResponseEntity.ok(ApiResponse.onSuccess(response));
    }
}
