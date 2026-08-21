package com.fmi.domain.comment.web.dto;

import java.time.LocalDateTime;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationDto {
    private String message;
    private Long postId;
    private String commenterName;
    private LocalDateTime createdAt;
}
