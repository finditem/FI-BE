package com.fmi.domain.report.web.dto;

import com.fmi.domain.report.data.enums.ReportStatus;
import lombok.Data;

@Data
public class ReportStatusUpdateRequestDTO {
    private ReportStatus status; // RESOLVED or REJECTED 등
    private String adminNote;
}


