package com.fmi.domain.inquirycomment.response;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InquiryCommentResponse {
    private Long id;
    private String content;
    private Long authorId;
    private String authorName;
    private String authorEmail;  // 비회원 식별용
    private Long parentId;
    private List<InquiryCommentResponse> replies;  // 대댓글 목록
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private boolean canEdit;
    private boolean canDelete;
}
