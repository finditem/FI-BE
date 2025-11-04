package com.fmi.domain.inquiry.web.dto.response;

import com.fmi.domain.inquiry.data.enums.InquiryCategory;
import com.fmi.domain.inquiry.data.enums.InquiryStatus;
import com.fmi.domain.inquiry.data.enums.InquiryType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InquiryListDTO {
    private Long inquiryId;
    private String title;
    private InquiryType inquiryType;
    private InquiryCategory category;
    private InquiryStatus status;
    private String authorNickname;
    private Boolean hasReply;
    private LocalDateTime createdAt;
}

