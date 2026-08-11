package com.fmi.domain.comment.web.dto;

import lombok.*;

import java.time.LocalDateTime;

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
