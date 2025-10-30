package com.fmi.domain.inquiry.web.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InquiryReplyDTO {
    private Long replyId;
    private String content;
    private String adminName;
    private LocalDateTime createdAt;
}

