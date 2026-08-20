package com.fmi.domain.inquirycomment.web.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateInquiryCommentDto {
    private String content;
    private Long parentId; // 대댓글인 경우 부모 댓글 ID
}
