package com.fmi.domain.notice.comment.web.controller;

import com.fmi.domain.notice.comment.service.NoticeCommentService;
import com.fmi.domain.notice.comment.web.dto.request.NoticeCommentCreateRequest;
import com.fmi.domain.notice.comment.web.dto.response.NoticeCommentPageResponse;
import com.fmi.domain.notice.comment.web.dto.response.NoticeCommentResponse;
import com.fmi.domain.notice.comment.web.dto.response.NoticeCommentSliceResponse;
import com.fmi.domain.notice.comment.web.swagger.NoticeCommentSwagger;
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

@RestController
@RequestMapping("/notices")
@RequiredArgsConstructor
public class NoticeCommentController implements NoticeCommentSwagger {

    private final NoticeCommentService noticeCommentService;

    @PostMapping(value = "/{noticeId}/comments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Override
    public ResponseEntity<ApiResponse<NoticeCommentResponse>> createComment(
            @PathVariable Long noticeId,
            @RequestPart("request") @Valid NoticeCommentCreateRequest request,
            @RequestPart(value = "images", required = false) List<MultipartFile> images,
            @AuthenticationPrincipal UserDetails userDetails) {

        NoticeCommentResponse response = noticeCommentService.createComment(noticeId, request, images, userDetails);
        return ResponseEntity.ok(ApiResponse.onSuccess(response));
    }

    @GetMapping("/{noticeId}/comments")
    @Override
    public ResponseEntity<ApiResponse<NoticeCommentSliceResponse>> getComments(
            @PathVariable Long noticeId,
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal UserDetails userDetails) {

        NoticeCommentSliceResponse response = noticeCommentService.getComments(noticeId, cursor, size, userDetails);
        return ResponseEntity.ok(ApiResponse.onSuccess(response));
    }

    @GetMapping("/comments/{commentId}/replies")
    @Override
    public ResponseEntity<ApiResponse<NoticeCommentPageResponse>> getReplies(
            @PathVariable Long commentId,
            @RequestParam(defaultValue = "0") int page,
            @AuthenticationPrincipal UserDetails userDetails) {

        NoticeCommentPageResponse response = noticeCommentService.getReplies(commentId, page, userDetails);
        return ResponseEntity.ok(ApiResponse.onSuccess(response));
    }

    @PutMapping(value = "/comments/{commentId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Override
    public ResponseEntity<ApiResponse<NoticeCommentResponse>> updateComment(
            @PathVariable Long commentId,
            @RequestPart("request") @Valid NoticeCommentCreateRequest request,
            @RequestPart(value = "images", required = false) List<MultipartFile> images,
            @AuthenticationPrincipal UserDetails userDetails) {

        NoticeCommentResponse response = noticeCommentService.updateComment(commentId, request, images, userDetails);
        return ResponseEntity.ok(ApiResponse.onSuccess(response));
    }

    @DeleteMapping("/comments/{commentId}")
    @Override
    public ResponseEntity<ApiResponse<Void>> deleteComment(
            @PathVariable Long commentId, @AuthenticationPrincipal UserDetails userDetails) {

        noticeCommentService.deleteComment(commentId, userDetails);
        return ResponseEntity.ok(ApiResponse.onSuccess(null));
    }
}
