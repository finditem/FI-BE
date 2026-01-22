package com.fmi.domain.admin.dto;

import com.fmi.domain.inquiry.data.enums.InquiryCategory;
import com.fmi.domain.inquiry.data.enums.InquiryStatus;
import com.fmi.domain.inquiry.data.enums.InquiryType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "관리자 문의 응답")
public class AdminInquiryResponse {

    @Schema(description = "문의 ID", example = "1")
    private Long inquiryId;
    @Schema(description = "제목", example = "계정 관련 문의")
    private String title;
    @Schema(description = "문의 타입", example = "PRIVATE")
    private InquiryType inquiryType;
    @Schema(description = "카테고리", example = "ACCOUNT")
    private InquiryCategory category;
    @Schema(description = "처리 상태", example = "ANSWERED")
    private InquiryStatus status;
    @Schema(description = "생성 시간", example = "2024-01-01T00:00:00")
    private LocalDateTime createdAt;

    @Schema(description = "사용자 ID", example = "1")
    private Long userId;
    @Schema(description = "사용자 닉네임", example = "홍길동")
    private String userNickname;
    @Schema(description = "사용자 이메일", example = "user@example.com")
    private String userEmail;
}

