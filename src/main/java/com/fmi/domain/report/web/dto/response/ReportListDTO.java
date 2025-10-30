package com.fmi.domain.report.web.dto.response;

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
public class ReportListDTO {
    private Long reportId;
    private ReportTargetType targetType;
    private Long targetId;
    private String targetTitle;        // 신고 대상의 제목/내용 일부
    private ReportType reportType;
    private ReportStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime resolvedAt;
}

