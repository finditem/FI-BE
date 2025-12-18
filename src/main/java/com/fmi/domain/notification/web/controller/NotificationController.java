package com.fmi.domain.notification.web.controller;

import com.fmi.domain.auth.data.User;
import com.fmi.domain.notification.service.NotificationService;
import com.fmi.domain.notification.web.dto.response.NotificationListDTO;
import com.fmi.domain.notification.web.dto.response.NotificationSettingsDTO;
import com.fmi.global.apiPayload.code.status.ErrorStatus;
import com.fmi.global.apiPayload.exception.GeneralException;
import com.fmi.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
    private static final int MAX_BATCH_SIZE = 1000;
    
    /**
     * 내 알림 목록 조회
     * GET /api/notification?unreadOnly=false&page=0&size=20
     */
    @GetMapping
    @Operation(summary = "내 알림 목록 조회", description = "읽음/읽지 않음 필터링 가능")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "알림 목록 조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "COMMON401: 인증이 필요합니다",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = "{\"isSuccess\": false, \"code\": \"COMMON401\", \"message\": \"인증이 필요합니다.\"}"
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "COMMON500: 서버 에러",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = "{\"isSuccess\": false, \"code\": \"COMMON500\", \"message\": \"서버 에러, 관리자에게 문의 바랍니다.\"}"
                            )
                    )
            )
    })
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
     * 알림 설정 조회
     * GET /api/notification/settings
     */
    @GetMapping("/settings")
    @Operation(summary = "내 알림 설정 조회")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "알림 설정 조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "COMMON401: 인증이 필요합니다",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = "{\"isSuccess\": false, \"code\": \"COMMON401\", \"message\": \"인증이 필요합니다.\"}"
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "COMMON500: 서버 에러",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = "{\"isSuccess\": false, \"code\": \"COMMON500\", \"message\": \"서버 에러, 관리자에게 문의 바랍니다.\"}"
                            )
                    )
            )
    })
    public ApiResponse<NotificationSettingsDTO> getSettings(@AuthenticationPrincipal User user) {
        NotificationSettingsDTO settings = notificationService.getSettings(user);
        return ApiResponse.onSuccess(settings);
    }
    
    /**
     * 알림 설정 변경(댓글/채팅/카테고리만 변경 가능)
     * PUT /notification/settings
     */
    @PutMapping("/settings")
    @Operation(summary = "알림 설정 변경", description = "댓글, 채팅, 카테고리 설정만 변경 가능합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "알림 설정 변경 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "COMMON400: 잘못된 요청입니다",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = "{\"isSuccess\": false, \"code\": \"COMMON400\", \"message\": \"잘못된 요청입니다.\"}"
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "COMMON401: 인증이 필요합니다",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = "{\"isSuccess\": false, \"code\": \"COMMON401\", \"message\": \"인증이 필요합니다.\"}"
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "COMMON500: 서버 에러",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = "{\"isSuccess\": false, \"code\": \"COMMON500\", \"message\": \"서버 에러, 관리자에게 문의 바랍니다.\"}"
                            )
                    )
            )
    })
    public ApiResponse<NotificationSettingsDTO> updateSettings(
            @AuthenticationPrincipal User user,
            @RequestBody BasicSettingsRequest request) {
        NotificationSettingsDTO settings = notificationService.updateBasicSettings(user,
                request.getCommentEnabled(), request.getChatEnabled(), request.getCategoryEnabled());
        return ApiResponse.onSuccess(settings);
    }

    // 통합으로 대체: PATCH /notification/settings/basic 제거

    public static class BasicSettingsRequest {
        private Boolean commentEnabled;
        private Boolean chatEnabled;
        private Boolean categoryEnabled;

        public Boolean getCommentEnabled() { return commentEnabled; }
        public void setCommentEnabled(Boolean commentEnabled) { this.commentEnabled = commentEnabled; }
        public Boolean getChatEnabled() { return chatEnabled; }
        public void setChatEnabled(Boolean chatEnabled) { this.chatEnabled = chatEnabled; }
        public Boolean getCategoryEnabled() { return categoryEnabled; }
        public void setCategoryEnabled(Boolean categoryEnabled) { this.categoryEnabled = categoryEnabled; }
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
                    responseCode = "400",
                    description = "COMMON400: 잘못된 요청입니다",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = "{\"isSuccess\": false, \"code\": \"COMMON400\", \"message\": \"잘못된 요청입니다.\"}"
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "COMMON401: 인증이 필요합니다",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = "{\"isSuccess\": false, \"code\": \"COMMON401\", \"message\": \"인증이 필요합니다.\"}"
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
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "COMMON500: 서버 에러",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = "{\"isSuccess\": false, \"code\": \"COMMON500\", \"message\": \"서버 에러, 관리자에게 문의 바랍니다.\"}"
                            )
                    )
            )
    })
    public ApiResponse<String> markAsReadBatch(
            @AuthenticationPrincipal User user,
            @RequestBody IdsRequest request
    ) {
        validateBatch(request);
        int updated = notificationService.markAsReadBatch(user, request.getIds());
        return ApiResponse.onSuccess(updated + "개의 알림을 읽음 처리했습니다.");
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
                    responseCode = "400",
                    description = "COMMON400: 잘못된 요청입니다",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = "{\"isSuccess\": false, \"code\": \"COMMON400\", \"message\": \"잘못된 요청입니다.\"}"
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "COMMON401: 인증이 필요합니다",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = "{\"isSuccess\": false, \"code\": \"COMMON401\", \"message\": \"인증이 필요합니다.\"}"
                            )
                    )
            ),
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
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "COMMON500: 서버 에러",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = "{\"isSuccess\": false, \"code\": \"COMMON500\", \"message\": \"서버 에러, 관리자에게 문의 바랍니다.\"}"
                            )
                    )
            )
    })
    public ApiResponse<String> deleteBatch(
            @AuthenticationPrincipal User user,
            @RequestBody IdsRequest request
    ) {
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

