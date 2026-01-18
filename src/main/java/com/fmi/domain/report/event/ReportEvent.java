package com.fmi.domain.report.event;

import com.fmi.domain.report.data.enums.ReportTargetType;
import com.fmi.domain.report.data.enums.ReportType;

import java.time.LocalDateTime;

public record ReportEvent(
        Long reportId,
        Long targetId,
        ReportTargetType targetType,
        ReportType reportType,
        String reason,
        Long reporterId,
        String reporterNickname,
        LocalDateTime createdAt
) {}
