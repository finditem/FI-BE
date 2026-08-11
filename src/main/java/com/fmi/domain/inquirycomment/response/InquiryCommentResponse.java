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
    private String authorEmail;
    private String profileImg;
    private boolean isAdmin;
    private Long parentId;
    @Builder.Default
    private List<InquiryCommentResponse> replies = List.of();
    private List<String> imageList;
    private boolean canEdit;
    private boolean canDelete;
    private LocalDateTime createdAt;
}
