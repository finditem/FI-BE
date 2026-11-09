package com.fmi.domain.notification.web.controller;

import com.fmi.domain.notification.data.enums.NotificationType;
import com.fmi.domain.notification.service.NotificationService;
import com.fmi.domain.notification.service.NotificationSubscriptionService;
import com.fmi.domain.notification.web.dto.response.NotificationListResponse;
import com.fmi.domain.notification.web.swagger.NotificationSwagger;
import com.fmi.domain.user.user.data.User;
import com.fmi.domain.user.user.service.UserQueryService;
import com.fmi.global.apiPayload.ApiResponse;
import com.fmi.global.apiPayload.CursorPageResponse;
import com.fmi.global.apiPayload.code.status.ErrorStatus;
import com.fmi.global.apiPayload.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class NotificationController implements NotificationSwagger {
    private final NotificationSubscriptionService notificationSubscriptionService;

    private final NotificationService notificationService;

    private final UserQueryService userQueryService;

    private static final int MAX_BATCH_SIZE = 1000;

    /**
     * SSE 실시간 알림 구독
     * GET /api/notifications/subscribe
     */
    @GetMapping(value = "/subscribe", produces = "text/event-stream")
    @Override
    public SseEmitter subscribe(@AuthenticationPrincipal UserDetails userDetails) {
        return notificationSubscriptionService.subscribe(userDetails);
    }

    /**
     * 내 알림 목록 조회
     * GET /api/notifications?unreadOnly=false&cursor=10&size=20
     */
    @GetMapping
    @Override
    public ApiResponse<CursorPageResponse<NotificationListResponse>> getMyNotifications(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false, defaultValue = "false") Boolean unreadOnly,
            @RequestParam(required = false) NotificationType notificationType,
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "20") int size) {

        User user = userQueryService.findUser(userDetails.getUsername());
        CursorPageResponse<NotificationListResponse> notifications =
                notificationService.getMyNotificationsCursor(user, unreadOnly, notificationType, cursor, size);
        return ApiResponse.onSuccess(notifications);
    }

    /**
     * 알림 다건 읽음 처리
     * PUT /notification/read-batch
     */
    @PutMapping("/read-batch")
    @Override
    public ApiResponse<String> markAsReadBatch(
            @AuthenticationPrincipal UserDetails userDetails, @RequestBody IdsRequest request) {
        User user = userQueryService.findUser(userDetails.getUsername());
        validateBatch(request);
        int updated = notificationService.markAsReadBatch(user, request.getIds());
        return ApiResponse.onSuccess(updated + "개의 알림을 읽음 처리했습니다.");
    }

    /**
     * 알림 전체 삭제
     * DELETE /notifications/all
     */
    @DeleteMapping("/all")
    @Override
    public ApiResponse<String> deleteAll(@AuthenticationPrincipal UserDetails userDetails) {
        User user = userQueryService.findUser(userDetails.getUsername());
        notificationService.deleteAllNotifications(user);
        return ApiResponse.onSuccess("모든 알림을 삭제했습니다.");
    }

    /**
     * 알림 다건 삭제
     * DELETE /notification/batch
     */
    @DeleteMapping("/batch")
    @Override
    public ApiResponse<String> deleteBatch(
            @AuthenticationPrincipal UserDetails userDetails, @RequestBody IdsRequest request) {
        User user = userQueryService.findUser(userDetails.getUsername());
        validateBatch(request);
        int deleted = notificationService.deleteBatch(user, request.getIds());
        return ApiResponse.onSuccess(deleted + "개의 알림을 삭제했습니다.");
    }

    private void validateBatch(IdsRequest request) {
        if (request == null || request.getIds() == null || request.getIds().isEmpty()) {
            throw new GeneralException(ErrorStatus._BAD_REQUEST);
        }
        if (request.getIds().size() > MAX_BATCH_SIZE) {
            throw new GeneralException(ErrorStatus._BAD_REQUEST);
        }
    }

    public static class IdsRequest {
        private java.util.List<Long> ids;

        public java.util.List<Long> getIds() {
            return ids;
        }

        public void setIds(java.util.List<Long> ids) {
            this.ids = ids;
        }
    }
}
