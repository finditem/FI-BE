package com.fmi.domain.notice.comment.web.controller;

import com.fmi.domain.notice.comment.service.NoticeCommentLikeService;
import com.fmi.global.apiPayload.ApiResponse;
import com.fmi.global.apiPayload.code.status.ErrorStatus;
import com.fmi.global.apiPayload.exception.GeneralException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/notices")
@RequiredArgsConstructor
@Tag(name = "NoticeComment", description = "공지사항 댓글 API")
public class NoticeCommentLikeController {

    private final NoticeCommentLikeService noticeCommentLikeService;

    @PostMapping("/comments/{commentId}/like")
    @Operation(summary = "공지사항 댓글 추천 추가")
    public ResponseEntity<ApiResponse<Void>> addCommentLike(
            @PathVariable Long commentId, @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            throw new GeneralException(ErrorStatus._UNAUTHORIZED);
        }
        noticeCommentLikeService.addCommentLike(commentId, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.onSuccess(null));
    }

    @DeleteMapping("/comments/{commentId}/like")
    @Operation(summary = "공지사항 댓글 추천 취소")
    public ResponseEntity<ApiResponse<Void>> removeCommentLike(
            @PathVariable Long commentId, @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            throw new GeneralException(ErrorStatus._UNAUTHORIZED);
        }
        noticeCommentLikeService.removeCommentLike(commentId, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.onSuccess(null));
    }
}
