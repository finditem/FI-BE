package com.fmi.domain.report.event;

import com.fmi.domain.report.data.enums.ReportType;
import org.hibernate.tool.schema.TargetType;

import java.time.LocalDateTime;

public record ReportEvent(
        Long reportId,
        TargetType targetType,
        Long targetId,
        ReportType reportType,
        String reason,
        Long reporterId,
        String reporterNickname,
        LocalDateTime createdAt
) {}
