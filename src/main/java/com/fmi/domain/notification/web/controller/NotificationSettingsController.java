package com.fmi.domain.notification.web.controller;

import com.fmi.domain.notification.service.NotificationSettingsService;
import com.fmi.domain.notification.web.dto.response.NotificationSettingsResponse;
import com.fmi.domain.notification.web.swagger.NotificationSettingsSwagger;
import com.fmi.domain.user.user.data.User;
import com.fmi.domain.user.user.service.UserQueryService;
import com.fmi.global.apiPayload.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class NotificationSettingsController implements NotificationSettingsSwagger {

    private final NotificationSettingsService notificationSettingsService;

    private final UserQueryService userQueryService;

    /**
     * 알림 설정 조회
     * GET /api/notifications/settings
     */
    @GetMapping("/settings")
    @Override
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
    @Override
    public ApiResponse<NotificationSettingsResponse> updateSettings(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody com.fmi.domain.notification.web.dto.request.NotificationSettingsUpdateRequest request) {
        User user = userQueryService.findUser(userDetails.getUsername());
        NotificationSettingsResponse settings = notificationSettingsService.updateSettings(user, request);
        return ApiResponse.onSuccess(settings);
    }
}
