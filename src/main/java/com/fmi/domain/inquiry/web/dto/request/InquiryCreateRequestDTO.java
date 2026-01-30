package com.fmi.domain.inquiry.web.dto.request;

import com.fmi.domain.inquiry.data.enums.InquiryCategory;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class InquiryCreateRequestDTO {
    @NotBlank
    private String title;
    @NotBlank
    private String content;
    private InquiryCategory category;
    @Email(message = "올바른 이메일 형식이 아닙니다")
    private String email; // 비회원 문의 시 이메일 (비회원 필수)
}


