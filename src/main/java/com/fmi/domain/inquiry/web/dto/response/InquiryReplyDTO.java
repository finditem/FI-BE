package com.fmi.domain.inquiry.web.dto.response;

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
@Schema(description = "문의 답변 응답")
public class InquiryReplyDTO {
    @Schema(description = "답변 ID", example = "1")
    private Long replyId;
    @Schema(description = "답변 내용", example = "비밀번호 변경은 마이페이지에서 가능합니다.")
    private String content;
    @Schema(description = "관리자 이름", example = "관리자")
    private String adminName;
    @Schema(description = "생성 시간", example = "2024-01-01T00:00:00")
    private LocalDateTime createdAt;
}

