package com.fmi.domain.inquiry.web.dto.request;

import com.fmi.domain.inquiry.data.enums.InquiryCategory;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class InquiryCreateRequestDTO {
    @NotBlank
    private String title;
    @NotBlank
    private String content;
    private InquiryCategory category;
    private String email; // 비회원 문의 시 이메일 (선택)
}


