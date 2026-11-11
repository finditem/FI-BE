package com.fmi.domain.notice.comment.web.controller;

import com.fmi.domain.notice.comment.service.NoticeCommentLikeService;
import com.fmi.domain.notice.comment.web.swagger.NoticeCommentLikeSwagger;
import com.fmi.global.apiPayload.ApiResponse;
import com.fmi.global.apiPayload.code.status.ErrorStatus;
import com.fmi.global.apiPayload.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/notices")
@RequiredArgsConstructor
public class NoticeCommentLikeController implements NoticeCommentLikeSwagger {

    private final NoticeCommentLikeService noticeCommentLikeService;

    @PostMapping("/comments/{commentId}/like")
    @Override
    public ResponseEntity<ApiResponse<Void>> addCommentLike(
            @PathVariable Long commentId, @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            throw new GeneralException(ErrorStatus._UNAUTHORIZED);
        }
        noticeCommentLikeService.addCommentLike(commentId, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.onSuccess(null));
    }

    @DeleteMapping("/comments/{commentId}/like")
    @Override
    public ResponseEntity<ApiResponse<Void>> removeCommentLike(
            @PathVariable Long commentId, @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            throw new GeneralException(ErrorStatus._UNAUTHORIZED);
        }
        noticeCommentLikeService.removeCommentLike(commentId, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.onSuccess(null));
    }
}
