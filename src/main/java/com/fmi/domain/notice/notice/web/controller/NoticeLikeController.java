package com.fmi.domain.notice.notice.web.controller;

import com.fmi.domain.notice.notice.service.NoticeLikeService;
import com.fmi.domain.notice.notice.web.swagger.NoticeLikeSwagger;
import com.fmi.global.apiPayload.ApiResponse;
import com.fmi.global.apiPayload.code.status.ErrorStatus;
import com.fmi.global.apiPayload.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/notices")
@RequiredArgsConstructor
public class NoticeLikeController implements NoticeLikeSwagger {

    private final NoticeLikeService noticeLikeService;

    /**
     * 공지사항 추천(좋아요) 추가
     * POST /notices/{noticeId}/like
     */
    @PostMapping("/{noticeId}/like")
    @Override
    public ApiResponse<Void> addLike(
            @PathVariable Long noticeId,
            @org.springframework.security.core.annotation.AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            throw new GeneralException(ErrorStatus._UNAUTHORIZED);
        }
        noticeLikeService.addLike(noticeId, userDetails.getUsername());
        return ApiResponse.onSuccess(null);
    }

    /**
     * 공지사항 추천(좋아요) 취소
     * DELETE /notices/{noticeId}/like
     */
    @DeleteMapping("/{noticeId}/like")
    @Override
    public ApiResponse<Void> removeLike(
            @PathVariable Long noticeId,
            @org.springframework.security.core.annotation.AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            throw new GeneralException(ErrorStatus._UNAUTHORIZED);
        }
        noticeLikeService.removeLike(noticeId, userDetails.getUsername());
        return ApiResponse.onSuccess(null);
    }
}
