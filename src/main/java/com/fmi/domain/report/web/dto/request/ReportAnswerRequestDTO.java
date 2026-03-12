package com.fmi.domain.report.web.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ReportAnswerRequestDTO {
    @NotBlank
    @Schema(description = "관리자 답변 내용", example = "확인 후 조치하였습니다.")
    private String adminAnswer;
}
