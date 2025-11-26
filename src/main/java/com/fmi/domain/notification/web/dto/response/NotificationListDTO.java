package com.fmi.domain.notification.web.dto.response;

import com.fmi.domain.notification.data.enums.NotificationType;
import com.fmi.domain.notification.data.enums.ReferenceType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationListDTO {
    private Long notificationId;
    private NotificationType type;
    private String title;
    private String message;
    private ReferenceType referenceType;
    private Long referenceId;
    private Boolean isRead;
    private LocalDateTime createdAt;
}

