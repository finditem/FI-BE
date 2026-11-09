package com.fmi.domain.post.comment.web.controller;

import com.fmi.domain.post.comment.service.CommentLikeService;
import com.fmi.domain.post.comment.web.swagger.CommentLikeSwagger;
import com.fmi.global.apiPayload.ApiResponse;
import com.fmi.global.apiPayload.code.status.ErrorStatus;
import com.fmi.global.apiPayload.exception.GeneralException;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class CommentLikeController implements CommentLikeSwagger {

    private final CommentLikeService commentLikeService;

    @PostMapping("/comments/{commentId}/likes")
    @Override
    public ResponseEntity<ApiResponse<Boolean>> createLike(
            @PathVariable Long commentId, @AuthenticationPrincipal UserDetails userDetails) {
        if (Objects.isNull(userDetails)) {
            throw new GeneralException(ErrorStatus._UNAUTHORIZED);
        }

        commentLikeService.createLike(commentId, userDetails);

        return ResponseEntity.ok(ApiResponse.onSuccess(true));
    }

    @DeleteMapping("/comments/{commentId}/likes")
    @Override
    public ResponseEntity<ApiResponse<Boolean>> deleteLike(
            @PathVariable Long commentId, @AuthenticationPrincipal UserDetails userDetails) {
        if (Objects.isNull(userDetails)) {
            throw new GeneralException(ErrorStatus._UNAUTHORIZED);
        }

        commentLikeService.deleteLike(commentId, userDetails);

        return ResponseEntity.ok(ApiResponse.onSuccess(false));
    }
}
