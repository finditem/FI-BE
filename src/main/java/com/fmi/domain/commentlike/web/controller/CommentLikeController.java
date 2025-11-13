package com.fmi.domain.commentlike.web.controller;

import com.fmi.domain.commentlike.service.CommentLikeService;
import com.fmi.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class CommentLikeController {

    private final CommentLikeService commentLikeService;

    @Operation(summary = "좋아요 알림 토글형식")
    @PostMapping("/{commentId}/like")
    public ResponseEntity<ApiResponse<Boolean>> toggleLike(
            @PathVariable Long commentId,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        boolean isLikedNow = commentLikeService.toggleLike(commentId, userDetails);

        return ResponseEntity.ok(ApiResponse.onSuccess(isLikedNow));
    }

}
