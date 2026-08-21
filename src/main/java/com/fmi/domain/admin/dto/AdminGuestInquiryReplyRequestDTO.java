package com.fmi.domain.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AdminGuestInquiryReplyRequestDTO {
    @NotBlank @Schema(description = "답변 내용", example = "안녕하세요, 문의 주신 내용에 대해 답변 드립니다.")
    private String content;
}
