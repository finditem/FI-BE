package com.fmi.domain.notification.web.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class NotificationSettingsUpdateDTO {
    private Boolean commentEnabled;          // 댓글 알림
    private Boolean chatEnabled;             // 채팅 알림
    private Boolean inquiryReplyEnabled;     // 문의 답변 알림
    private Boolean reportResultEnabled;     // 신고 처리 결과 알림
    private Boolean favoriteEnabled;         // 좋아요 알림
    private Boolean noticeEnabled;           // 공지사항 알림
    private Boolean categoryEnabled;         // 카테고리 알림
    private Boolean marketingConsent;        // 마케팅 이메일 수신 동의
}

