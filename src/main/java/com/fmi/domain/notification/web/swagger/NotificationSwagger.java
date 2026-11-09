package com.fmi.domain.notification.web.swagger;

import com.fmi.domain.notification.data.enums.NotificationType;
import com.fmi.domain.notification.web.controller.NotificationController.IdsRequest;
import com.fmi.domain.notification.web.dto.response.NotificationListResponse;
import com.fmi.global.apiPayload.ApiResponse;
import com.fmi.global.apiPayload.CursorPageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Tag(name = "Notification", description = "알림 API")
public interface NotificationSwagger {

    @Operation(
            summary = "실시간 알림 구독 (SSE)",
            description = "Server-Sent Events를 통해 실시간으로 알림을 수신합니다. " + "연결 시 미읽은 알림을 자동으로 전송합니다. "
                    + "연결이 끊기면 클라이언트에서 자동으로 재연결해야 합니다.")
    @ApiResponses({@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "SSE 연결 성공")
    })
    SseEmitter subscribe(@Parameter(hidden = true) UserDetails userDetails);

    @Operation(
            summary = "내 알림 목록 조회",
            description = "커서 기반 페이지네이션으로 알림을 조회합니다. cursor를 생략하면 최신 항목부터 반환합니다. 읽음/읽지 않음 및 알림 타입별 필터링 가능")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "알림 목록 조회 성공")
    })
    ApiResponse<CursorPageResponse<NotificationListResponse>> getMyNotifications(
            @Parameter(hidden = true) UserDetails userDetails,
            Boolean unreadOnly,
            @Parameter(
                            description =
                                    "알림 타입 필터 (COMMENT, CHAT, CHAT_REMINDER, INQUIRY_REPLY, REPORT_RESULT, FAVORITE, CATEGORY, NOTICE, REVIEW, SYSTEM)")
                    NotificationType notificationType,
            Long cursor,
            int size);

    @Operation(summary = "알림 다건 읽음 처리")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "알림 읽음 처리 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "NOTIFICATION404-NOT_FOUND: 존재하지 않는 알림입니다",
                content =
                        @Content(
                                mediaType = "application/json",
                                examples =
                                        @ExampleObject(
                                                value =
                                                        "{\"isSuccess\": false, \"code\": \"NOTIFICATION404-NOT_FOUND\", \"message\": \"존재하지 않는 알림입니다.\"}")))
    })
    ApiResponse<String> markAsReadBatch(@Parameter(hidden = true) UserDetails userDetails, IdsRequest request);

    @Operation(summary = "알림 전체 삭제", description = "인증된 사용자의 모든 알림을 삭제합니다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "알림 전체 삭제 성공")
    })
    ApiResponse<String> deleteAll(@Parameter(hidden = true) UserDetails userDetails);

    @Operation(summary = "알림 다건 삭제")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "알림 삭제 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "403",
                description = "NOTIFICATION403-ACCESS_DENIED: 해당 알림에 접근할 권한이 없습니다",
                content =
                        @Content(
                                mediaType = "application/json",
                                examples =
                                        @ExampleObject(
                                                value =
                                                        "{\"isSuccess\": false, \"code\": \"NOTIFICATION403-ACCESS_DENIED\", \"message\": \"해당 알림에 접근할 권한이 없습니다.\"}"))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "NOTIFICATION404-NOT_FOUND: 존재하지 않는 알림입니다",
                content =
                        @Content(
                                mediaType = "application/json",
                                examples =
                                        @ExampleObject(
                                                value =
                                                        "{\"isSuccess\": false, \"code\": \"NOTIFICATION404-NOT_FOUND\", \"message\": \"존재하지 않는 알림입니다.\"}")))
    })
    ApiResponse<String> deleteBatch(@Parameter(hidden = true) UserDetails userDetails, IdsRequest request);
}
