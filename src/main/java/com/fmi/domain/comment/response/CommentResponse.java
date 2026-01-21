package com.fmi.domain.comment.response;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommentResponse {
    private Long id;
    private String content;
    private Long authorId;
    private String authorName;
    private LocalDateTime createdAt;
    private int likeCount;
    private Long parentId;
    private boolean canEdit;
    private boolean canDelete;
}