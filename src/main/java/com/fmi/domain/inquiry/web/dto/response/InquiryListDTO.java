package com.fmi.domain.inquiry.web.dto.response;

import com.fmi.domain.inquiry.data.enums.InquiryCategory;
import com.fmi.domain.inquiry.data.enums.InquiryStatus;
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
@Schema(description = "문의 목록 응답")
public class InquiryListDTO {
    @Schema(description = "문의 ID", example = "1")
    private Long inquiryId;
    @Schema(description = "제목", example = "계정 관련 문의")
    private String title;
    @Schema(description = "내용", example = "비밀번호를 변경하고 싶습니다.")
    private String content;
    @Schema(description = "카테고리", example = "ACCOUNT")
    private InquiryCategory category;
    @Schema(description = "처리 상태", example = "ANSWERED")
    private InquiryStatus status;
    @Schema(description = "생성 시간", example = "2024-01-01T00:00:00")
    private LocalDateTime createdAt;
}

