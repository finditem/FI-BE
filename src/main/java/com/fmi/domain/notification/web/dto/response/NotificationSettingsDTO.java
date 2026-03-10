package com.fmi.domain.notification.web.dto.response;

import com.fmi.domain.Enum.Category;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Set;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "알림 설정 응답")
public class NotificationSettingsDTO {
    @Schema(description = "댓글 알림 활성화 여부", example = "true")
    private Boolean commentEnabled;
    @Schema(description = "채팅 알림 활성화 여부", example = "true")
    private Boolean chatEnabled;
    @Schema(description = "문의 답변 알림 활성화 여부", example = "true")
    private Boolean inquiryReplyEnabled;
    @Schema(description = "신고 처리 결과 알림 활성화 여부", example = "true")
    private Boolean reportResultEnabled;
    @Schema(description = "좋아요 알림 활성화 여부", example = "true")
    private Boolean favoriteEnabled;
    @Schema(description = "공지사항 알림 활성화 여부", example = "true")
    private Boolean noticeEnabled;
    @Schema(description = "카테고리 키워드 알림 전체 활성화 여부", example = "true")
    private Boolean categoryEnabled;
    @Schema(description = "활성화된 카테고리 키워드 목록", example = "[\"ELECTRONICS\", \"WALLET\"]")
    private Set<Category> enabledCategories;
    @Schema(description = "전체 브라우저 알림 활성화 여부", example = "true")
    private Boolean browserNotificationEnabled;
    @Schema(description = "마케팅 이메일 수신 동의", example = "false")
    private Boolean marketingConsent;
}

