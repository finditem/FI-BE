package com.fmi.domain.inquiry.web.dto.request;

import com.fmi.domain.inquiry.data.enums.InquiryCategory;
import com.fmi.domain.inquiry.data.enums.InquiryType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class InquiryCreateRequestDTO {
    @NotBlank
    private String title;
    @NotBlank
    private String content;
    private InquiryCategory category;
    @NotNull
    private InquiryType inquiryType; // PUBLIC or PRIVATE
    private String email; // PUBLIC일 때만 사용(비회원 문의)
}


