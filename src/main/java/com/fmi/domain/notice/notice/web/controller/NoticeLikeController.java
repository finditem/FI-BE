package com.fmi.domain.notice.notice.web.controller;

import com.fmi.domain.notice.notice.service.NoticeLikeService;
import com.fmi.global.apiPayload.ApiResponse;
import com.fmi.global.apiPayload.code.status.ErrorStatus;
import com.fmi.global.apiPayload.exception.GeneralException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/notices")
@RequiredArgsConstructor
@Tag(name = "Notice", description = "공지사항 API")
public class NoticeLikeController {

    private final NoticeLikeService noticeLikeService;

    /**
     * 공지사항 추천(좋아요) 추가
     * POST /notices/{noticeId}/like
     */
    @PostMapping("/{noticeId}/like")
    @Operation(summary = "공지사항 추천 추가", description = "공지사항을 추천합니다.")
    @ApiResponses({@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "추천 성공")})
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
    @Operation(summary = "공지사항 추천 취소", description = "공지사항 추천을 취소합니다.")
    @ApiResponses({@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "추천 취소 성공")
    })
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
