package com.fmi.domain.inquiry.web.dto.response;

import com.fmi.domain.inquiry.data.enums.InquiryCategory;
import com.fmi.domain.inquiry.data.enums.InquiryStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InquiryDetailDTO {
    private Long inquiryId;
    private String title;
    private String content;
    private InquiryCategory category;
    private InquiryStatus status;
    private LocalDateTime createdAt;
    
    // 답변 (있는 경우)
    private InquiryReplyDTO reply;
}

