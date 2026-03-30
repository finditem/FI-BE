package com.fmi.domain.notification.converter;

import com.fmi.domain.notification.data.Notification;
import com.fmi.domain.notification.data.NotificationSettings;
import com.fmi.domain.notification.web.dto.response.NotificationListDTO;
import com.fmi.domain.notification.web.dto.response.NotificationSettingsDTO;
import org.springframework.stereotype.Component;

@Component
public class NotificationConverter {
    
    public NotificationListDTO toListDTO(Notification notification) {
        return NotificationListDTO.builder()
                .notificationId(notification.getNotificationId())
                .type(notification.getType())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .referenceType(notification.getReferenceType())
                .referenceId(notification.getReferenceId())
                .isRead(notification.getIsRead())
                .createdAt(notification.getCreatedAt())
                .build();
    }
    
    public NotificationSettingsDTO toSettingsDTO(NotificationSettings settings, com.fmi.domain.auth.data.User user) {
        return NotificationSettingsDTO.builder()
                .commentEnabled(settings.getCommentEnabled())
                .chatEnabled(settings.getChatEnabled())
                .inquiryReplyEnabled(settings.getInquiryReplyEnabled())
                .reportResultEnabled(settings.getReportResultEnabled())
                .favoriteEnabled(settings.getFavoriteEnabled())
                .noticeEnabled(settings.getNoticeEnabled())
                .categoryEnabled(settings.getCategoryEnabled())
                .enabledCategories(settings.getEnabledCategories())
                .browserNotificationEnabled(settings.getBrowserNotificationEnabled())
                .termsOfServiceAgreed(user.isTermsOfServiceAgreed())
                .contentPolicyAgreed(user.isContentPolicyAgreed())
                .marketingConsent(user.isMarketingConsent())
                .build();
    }
}

