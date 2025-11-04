package com.fmi.domain.report.web.dto;

import com.fmi.domain.report.data.enums.ReportStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ReportStatusUpdateRequestDTO {
    @NotNull
    private ReportStatus status; // RESOLVED or REJECTED 등
    private String adminNote;
}


