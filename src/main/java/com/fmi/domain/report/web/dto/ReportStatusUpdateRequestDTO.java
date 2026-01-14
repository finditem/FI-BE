package com.fmi.domain.report.web.dto;

import com.fmi.domain.report.data.enums.ReportStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ReportStatusUpdateRequestDTO {
    @NotNull
    @Schema(
            description = "신고 상태 (REVIEWED=처리중, RESOLVED=처리완료만 가능. 관리자가 신고를 처리 중 또는 처리 완료로 변경할 때 사용)",
            example = "RESOLVED"
    )
    private ReportStatus status;
    private String adminNote;
}


