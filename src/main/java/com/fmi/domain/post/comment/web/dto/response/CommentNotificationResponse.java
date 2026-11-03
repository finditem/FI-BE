package com.fmi.domain.post.comment.web.dto.response;

import java.time.LocalDateTime;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommentNotificationResponse {
    private String message;
    private Long postId;
    private String commenterName;
    private LocalDateTime createdAt;
}
