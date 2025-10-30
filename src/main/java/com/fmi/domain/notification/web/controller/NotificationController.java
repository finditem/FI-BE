package com.fmi.domain.notification.web.controller;

import com.fmi.domain.auth.data.User;
import com.fmi.domain.notification.service.NotificationService;
import com.fmi.domain.notification.web.dto.request.NotificationSettingsUpdateDTO;
import com.fmi.domain.notification.web.dto.response.NotificationListDTO;
import com.fmi.domain.notification.web.dto.response.NotificationSettingsDTO;
import com.fmi.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/notification")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
@Tag(name = "Notification", description = "알림 API")
public class NotificationController {
    
    private final NotificationService notificationService;
    
    /**
     * 내 알림 목록 조회
     * GET /api/notification?unreadOnly=false&page=0&size=20
     */
    @GetMapping
    @Operation(summary = "내 알림 목록 조회", description = "읽음/읽지 않음 필터링 가능")
    public ApiResponse<Page<NotificationListDTO>> getMyNotifications(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false, defaultValue = "false") Boolean unreadOnly,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<NotificationListDTO> notifications = notificationService.getMyNotifications(user, unreadOnly, pageable);
        return ApiResponse.onSuccess(notifications);
    }
    
    /**
     * 읽지 않은 알림 개수
     * GET /api/notification/unread
     */
    @GetMapping("/unread")
    @Operation(summary = "읽지 않은 알림 개수 조회")
    public ApiResponse<Long> getUnreadCount(@AuthenticationPrincipal User user) {
        Long count = notificationService.getUnreadCount(user);
        return ApiResponse.onSuccess(count);
    }
    
    /**
     * 알림 읽음 처리
     * PUT /api/notification/{notificationId}/read
     */
    @PutMapping("/{notificationId}/read")
    @Operation(summary = "알림 읽음 처리")
    public ApiResponse<String> markAsRead(
            @PathVariable Long notificationId,
            @AuthenticationPrincipal User user) {
        
        notificationService.markAsRead(notificationId, user);
        return ApiResponse.onSuccess("알림을 읽음 처리했습니다.");
    }
    
    /**
     * 모든 알림 읽음 처리
     * PUT /api/notification/read-all
     */
    @PutMapping("/read-all")
    @Operation(summary = "모든 알림 읽음 처리")
    public ApiResponse<String> markAllAsRead(@AuthenticationPrincipal User user) {
        int count = notificationService.markAllAsRead(user);
        return ApiResponse.onSuccess(count + "개의 알림을 읽음 처리했습니다.");
    }
    
    /**
     * 알림 삭제
     * DELETE /api/notification/{notificationId}
     */
    @DeleteMapping("/{notificationId}")
    @Operation(summary = "알림 삭제")
    public ApiResponse<String> deleteNotification(
            @PathVariable Long notificationId,
            @AuthenticationPrincipal User user) {
        
        notificationService.deleteNotification(notificationId, user);
        return ApiResponse.onSuccess("알림을 삭제했습니다.");
    }
    
    /**
     * 모든 알림 삭제
     * DELETE /api/notification/all
     */
    @DeleteMapping("/all")
    @Operation(summary = "모든 알림 삭제")
    public ApiResponse<String> deleteAllNotifications(@AuthenticationPrincipal User user) {
        notificationService.deleteAllNotifications(user);
        return ApiResponse.onSuccess("모든 알림을 삭제했습니다.");
    }
    
    /**
     * 알림 설정 조회
     * GET /api/notification/settings
     */
    @GetMapping("/settings")
    @Operation(summary = "내 알림 설정 조회")
    public ApiResponse<NotificationSettingsDTO> getSettings(@AuthenticationPrincipal User user) {
        NotificationSettingsDTO settings = notificationService.getSettings(user);
        return ApiResponse.onSuccess(settings);
    }
    
    /**
     * 알림 설정 변경
     * PUT /api/notification/settings
     */
    @PutMapping("/settings")
    @Operation(summary = "알림 설정 변경", description = "받고 싶은 알림 유형을 선택할 수 있습니다.")
    public ApiResponse<NotificationSettingsDTO> updateSettings(
            @AuthenticationPrincipal User user,
            @RequestBody NotificationSettingsUpdateDTO request) {
        
        NotificationSettingsDTO settings = notificationService.updateSettings(user, request);
        return ApiResponse.onSuccess(settings);
    }
}

