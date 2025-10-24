package com.fmi.domain.comment.web.controller;

import com.fmi.domain.comment.response.CommentResponse;
import com.fmi.domain.comment.service.CommentService;
import com.fmi.domain.comment.web.dto.CreateCommentDto;
import com.fmi.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequestMapping("comment")
@RestController
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @PostMapping(value = "/{postId}")
    @Operation(summary = "댓글 생성")
    public ResponseEntity<ApiResponse<CommentResponse>> createComment(
            @RequestBody CreateCommentDto request,
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long postId) {

        CommentResponse response = commentService.createComment(request,userDetails,postId);

        return ResponseEntity.ok(ApiResponse.onSuccess(response));
    }

    @GetMapping(value = "/{postId}")
    @Operation(summary = "게시글 별 댓글 조회")
    public ResponseEntity<ApiResponse<List<CommentResponse>>> getAllComment(
            @PathVariable Long postId) {

        List<CommentResponse> response = commentService.getComment(postId);

        return ResponseEntity.ok(ApiResponse.onSuccess(response));
    }

    @PutMapping(value = "/{commentId}")
    @Operation(summary = "댓글 수정")
    public ResponseEntity<ApiResponse<CommentResponse>> updateComment(
            @RequestBody CreateCommentDto request,
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long commentId) {

        CommentResponse response = commentService.updateComment(request,userDetails,commentId);

        return ResponseEntity.ok(ApiResponse.onSuccess(response));
    }

    @DeleteMapping(value = "/{commentId}")
    @Operation(summary = "댓글 삭제")
    public ResponseEntity<ApiResponse<CommentResponse>> deleteComment(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long commentId) {

        CommentResponse response = commentService.deleteComment(userDetails,commentId);

        return ResponseEntity.ok(ApiResponse.onSuccess(response));
    }
}
