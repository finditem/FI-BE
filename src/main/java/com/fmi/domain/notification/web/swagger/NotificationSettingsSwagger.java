package com.fmi.domain.notification.web.swagger;

import com.fmi.domain.notification.web.dto.response.NotificationSettingsResponse;
import com.fmi.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.userdetails.UserDetails;

@Tag(name = "Notification", description = "알림 API")
public interface NotificationSettingsSwagger {

    @Operation(summary = "내 알림 설정 조회")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "알림 설정 조회 성공")
    })
    ApiResponse<NotificationSettingsResponse> getSettings(@Parameter(hidden = true) UserDetails userDetails);

    @Operation(
            summary = "알림 설정 변경",
            description = "모든 알림 타입을 개별적으로 설정/해제할 수 있습니다. "
                    + "지원 필드: 댓글, 채팅, 즐겨찾기, 1:1문의 답변, 신고 처리 결과, 공지사항, 카테고리, 마케팅 이메일 수신 동의")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "알림 설정 변경 성공")
    })
    ApiResponse<NotificationSettingsResponse> updateSettings(
            @Parameter(hidden = true) UserDetails userDetails,
            com.fmi.domain.notification.web.dto.request.NotificationSettingsUpdateRequest request);
}
