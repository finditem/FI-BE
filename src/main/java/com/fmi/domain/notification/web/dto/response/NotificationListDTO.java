package com.fmi.domain.notification.web.dto.response;

import com.fmi.domain.notification.data.enums.NotificationType;
import com.fmi.domain.notification.data.enums.ReferenceType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "알림 목록 응답")
public class NotificationListDTO {
    @Schema(description = "알림 ID", example = "1")
    private Long notificationId;
    @Schema(description = "알림 타입", example = "COMMENT")
    private NotificationType type;
    @Schema(description = "알림 제목", example = "새 댓글이 달렸습니다")
    private String title;
    @Schema(description = "알림 메시지", example = "게시글에 댓글이 달렸습니다.")
    private String message;
    @Schema(description = "참조 타입", example = "POST")
    private ReferenceType referenceType;
    @Schema(description = "참조 ID", example = "1")
    private Long referenceId;
    @Schema(description = "읽음 여부", example = "false")
    private Boolean isRead;
    @Schema(description = "생성 시간", example = "2024-01-01T00:00:00")
    private LocalDateTime createdAt;
}

