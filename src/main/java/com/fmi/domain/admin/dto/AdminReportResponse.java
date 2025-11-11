package com.fmi.domain.admin.dto;

import com.fmi.domain.report.data.enums.ReportStatus;
import com.fmi.domain.report.data.enums.ReportTargetType;
import com.fmi.domain.report.data.enums.ReportType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminReportResponse {

    private Long reportId;
    private ReportTargetType targetType;
    private Long targetId;
    private ReportType reportType;
    private ReportStatus status;
    private String reason;
    private String adminNote;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime resolvedAt;

    private Long reporterId;
    private String reporterNickname;
    private String reporterEmail;
}

