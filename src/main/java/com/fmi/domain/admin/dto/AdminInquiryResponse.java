package com.fmi.domain.admin.dto;

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
public class AdminInquiryResponse {

    private Long inquiryId;
    private String title;
    private InquiryType inquiryType;
    private InquiryCategory category;
    private InquiryStatus status;
    private LocalDateTime createdAt;

    private Long userId;
    private String userNickname;
    private String userEmail;
}

