package com.fmi.domain.report.web.dto;

import com.fmi.domain.report.data.enums.ReportStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ReportStatusUpdateRequestDTO {
    @NotNull @Schema(description = "신고 상태 (PENDING=대기, REVIEWED=처리중, RESOLVED=처리완료)", example = "RESOLVED")
    private ReportStatus status;
}
