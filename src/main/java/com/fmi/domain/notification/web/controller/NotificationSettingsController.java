package com.fmi.domain.notification.web.controller;

import com.fmi.domain.notification.service.NotificationSettingsService;
import com.fmi.domain.notification.web.dto.response.NotificationSettingsResponse;
import com.fmi.domain.user.user.data.User;
import com.fmi.domain.user.user.service.UserQueryService;
import com.fmi.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
@Tag(name = "Notification", description = "알림 API")
public class NotificationSettingsController {

    private final NotificationSettingsService notificationSettingsService;

    private final UserQueryService userQueryService;

    /**
     * 알림 설정 조회
     * GET /api/notifications/settings
     */
    @GetMapping("/settings")
    @Operation(summary = "내 알림 설정 조회")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "알림 설정 조회 성공")
    })
    public ApiResponse<NotificationSettingsResponse> getSettings(@AuthenticationPrincipal UserDetails userDetails) {
        User user = userQueryService.findUser(userDetails.getUsername());
        NotificationSettingsResponse settings = notificationSettingsService.getSettings(user);
        return ApiResponse.onSuccess(settings);
    }

    /**
     * 알림 설정 변경 (모든 알림 타입 변경 가능)
     * PUT /notifications/settings
     */
    @PutMapping("/settings")
    @Operation(
            summary = "알림 설정 변경",
            description = "모든 알림 타입을 개별적으로 설정/해제할 수 있습니다. "
                    + "지원 필드: 댓글, 채팅, 즐겨찾기, 1:1문의 답변, 신고 처리 결과, 공지사항, 카테고리, 마케팅 이메일 수신 동의")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "알림 설정 변경 성공")
    })
    public ApiResponse<NotificationSettingsResponse> updateSettings(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody com.fmi.domain.notification.web.dto.request.NotificationSettingsUpdateRequest request) {
        User user = userQueryService.findUser(userDetails.getUsername());
        NotificationSettingsResponse settings = notificationSettingsService.updateSettings(user, request);
        return ApiResponse.onSuccess(settings);
    }
}
