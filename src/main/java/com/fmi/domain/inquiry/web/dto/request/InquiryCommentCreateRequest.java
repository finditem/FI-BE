package com.fmi.domain.inquiry.web.dto.request;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class InquiryCommentCreateRequest {
    private String content;
    private Long parentId; // 대댓글인 경우 부모 댓글 ID
}
