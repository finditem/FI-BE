package com.fmi.domain.report.event;

import com.fmi.domain.report.data.Report;
import com.fmi.domain.report.data.enums.ReportTargetType;
import com.fmi.domain.report.data.enums.ReportType;
import com.fmi.domain.user.data.User;
import java.time.LocalDateTime;

public record ReportEvent(
        Long reportId,
        Long targetId,
        ReportTargetType targetType,
        ReportType reportType,
        String reason,
        Long reporterId,
        String reporterNickname,
        LocalDateTime createdAt) {
    public static ReportEvent from(Report report, User reporter) {
        return new ReportEvent(
                report.getReportId(),
                report.getTargetId(),
                report.getTargetType(),
                report.getReportType(),
                report.getReason(),
                reporter.getId(),
                reporter.getNickname(),
                report.getCreatedAt());
    }
}
