package com.fmi.domain.noticecomment.response;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NoticeCommentResponse {
    private Long id;
    private String content;
    private Long authorId;
    private String authorName;
    private LocalDateTime createdAt;
    private Long parentId;
    private boolean canEdit;
    private boolean canDelete;
}
