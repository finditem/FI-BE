package com.fmi.domain.notification.web.controller;

import com.fmi.domain.auth.data.User;
import com.fmi.domain.auth.repository.UserRepository;
import com.fmi.domain.notification.data.enums.NotificationType;
import com.fmi.domain.notification.service.NotificationService;
import com.fmi.domain.notification.service.SseEmitterService;
import com.fmi.domain.notification.web.dto.response.NotificationListDTO;
import com.fmi.domain.notification.web.dto.response.NotificationSettingsDTO;
import com.fmi.global.apiPayload.CursorPageResponse;
import com.fmi.global.apiPayload.code.status.ErrorStatus;
import com.fmi.global.apiPayload.exception.GeneralException;
import com.fmi.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Notification", description = "알림 API")
public class NotificationController {

    private final NotificationService notificationService;
    private final SseEmitterService sseEmitterService;
    private final UserRepository userRepository;
    private static final int MAX_BATCH_SIZE = 1000;
    
    /**
     * SSE 실시간 알림 구독
     * GET /api/notifications/subscribe
     */
    @GetMapping(value = "/subscribe", produces = "text/event-stream")
    @Operation(summary = "실시간 알림 구독 (SSE)",
               description = "Server-Sent Events를 통해 실시간으로 알림을 수신합니다. " +
                           "연결 시 미읽은 알림을 자동으로 전송합니다. " +
                           "연결이 끊기면 클라이언트에서 자동으로 재연결해야 합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "SSE 연결 성공")
    })
    public SseEmitter subscribe(@AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new GeneralException(ErrorStatus._USER_NOT_FOUND));
        SseEmitter emitter = sseEmitterService.subscribe(user.getId());

        // 재연결 시 미읽은 알림 전송
        sseEmitterService.sendUnreadNotifications(user.getId(),
                notificationService.getUnreadNotificationDTOs(user));

        return emitter;
    }

    /**
     * 내 알림 목록 조회
     * GET /api/notifications?unreadOnly=false&cursor=10&size=20
     */
    @GetMapping
    @Operation(summary = "내 알림 목록 조회", description = "커서 기반 페이지네이션으로 알림을 조회합니다. cursor를 생략하면 최신 항목부터 반환합니다. 읽음/읽지 않음 및 알림 타입별 필터링 가능")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "알림 목록 조회 성공")
    })
    public ApiResponse<CursorPageResponse<NotificationListDTO>> getMyNotifications(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false, defaultValue = "false") Boolean unreadOnly,
            @Parameter(description = "알림 타입 필터 (COMMENT, REPLY, MENTION, CHAT, CHAT_REMINDER, INQUIRY_REPLY, REPORT_RESULT, FAVORITE, CATEGORY, NOTICE, SYSTEM, LIKE)")
            @RequestParam(required = false) NotificationType notificationType,
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "20") int size) {

        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new GeneralException(ErrorStatus._USER_NOT_FOUND));
        CursorPageResponse<NotificationListDTO> notifications = notificationService.getMyNotificationsCursor(user, unreadOnly, notificationType, cursor, size);
        return ApiResponse.onSuccess(notifications);
    }
    
    
    
    /**
     * 알림 설정 조회
     * GET /api/notifications/settings
     */
    @GetMapping("/settings")
    @Operation(summary = "내 알림 설정 조회")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "알림 설정 조회 성공")
    })
    public ApiResponse<NotificationSettingsDTO> getSettings(@AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new GeneralException(ErrorStatus._USER_NOT_FOUND));
        NotificationSettingsDTO settings = notificationService.getSettings(user);
        return ApiResponse.onSuccess(settings);
    }
    
    /**
     * 알림 설정 변경 (모든 알림 타입 변경 가능)
     * PUT /notifications/settings
     */
    @PutMapping("/settings")
    @Operation(summary = "알림 설정 변경", 
               description = "모든 알림 타입을 개별적으로 설정/해제할 수 있습니다. " +
                           "지원 필드: 댓글, 채팅, 즐겨찾기, 1:1문의 답변, 신고 처리 결과, 공지사항, 카테고리, 마케팅 이메일 수신 동의")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "알림 설정 변경 성공")
    })
    public ApiResponse<NotificationSettingsDTO> updateSettings(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody com.fmi.domain.notification.web.dto.request.NotificationSettingsUpdateDTO request) {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new GeneralException(ErrorStatus._USER_NOT_FOUND));
        NotificationSettingsDTO settings = notificationService.updateSettings(user, request);
        return ApiResponse.onSuccess(settings);
    }

    /**
     * 알림 다건 읽음 처리
     * PUT /notification/read-batch
     */
    @PutMapping("/read-batch")
    @Operation(summary = "알림 다건 읽음 처리")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "알림 읽음 처리 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "NOTIFICATION404-NOT_FOUND: 존재하지 않는 알림입니다",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = "{\"isSuccess\": false, \"code\": \"NOTIFICATION404-NOT_FOUND\", \"message\": \"존재하지 않는 알림입니다.\"}"
                            )
                    )
            )
    })
    public ApiResponse<String> markAsReadBatch(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody IdsRequest request
    ) {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new GeneralException(ErrorStatus._USER_NOT_FOUND));
        validateBatch(request);
        int updated = notificationService.markAsReadBatch(user, request.getIds());
        return ApiResponse.onSuccess(updated + "개의 알림을 읽음 처리했습니다.");
    }

    /**
     * 알림 전체 삭제
     * DELETE /notifications/all
     */
    @DeleteMapping("/all")
    @Operation(summary = "알림 전체 삭제", description = "인증된 사용자의 모든 알림을 삭제합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "알림 전체 삭제 성공")
    })
    public ApiResponse<String> deleteAll(@AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new GeneralException(ErrorStatus._USER_NOT_FOUND));
        notificationService.deleteAllNotifications(user);
        return ApiResponse.onSuccess("모든 알림을 삭제했습니다.");
    }

    /**
     * 알림 다건 삭제
     * DELETE /notification/batch
     */
    @DeleteMapping("/batch")
    @Operation(summary = "알림 다건 삭제")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "알림 삭제 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "NOTIFICATION403-ACCESS_DENIED: 해당 알림에 접근할 권한이 없습니다",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = "{\"isSuccess\": false, \"code\": \"NOTIFICATION403-ACCESS_DENIED\", \"message\": \"해당 알림에 접근할 권한이 없습니다.\"}"
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "NOTIFICATION404-NOT_FOUND: 존재하지 않는 알림입니다",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = "{\"isSuccess\": false, \"code\": \"NOTIFICATION404-NOT_FOUND\", \"message\": \"존재하지 않는 알림입니다.\"}"
                            )
                    )
            )
    })
    public ApiResponse<String> deleteBatch(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody IdsRequest request
    ) {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new GeneralException(ErrorStatus._USER_NOT_FOUND));
        validateBatch(request);
        int deleted = notificationService.deleteBatch(user, request.getIds());
        return ApiResponse.onSuccess(deleted + "개의 알림을 삭제했습니다.");
    }

    public static class IdsRequest {
        private java.util.List<Long> ids;
        public java.util.List<Long> getIds() { return ids; }
        public void setIds(java.util.List<Long> ids) { this.ids = ids; }
    }

    private void validateBatch(IdsRequest request) {
        if (request == null || request.getIds() == null || request.getIds().isEmpty()) {
            throw new GeneralException(ErrorStatus._BAD_REQUEST);
        }
        if (request.getIds().size() > MAX_BATCH_SIZE) {
            throw new GeneralException(ErrorStatus._BAD_REQUEST);
        }
    }
}

