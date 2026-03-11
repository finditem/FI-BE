package com.fmi.domain.report.converter;

import com.fmi.domain.report.data.Report;
import com.fmi.domain.report.web.dto.response.ReportDetailDTO;
import com.fmi.domain.report.web.dto.response.ReportListDTO;
import com.fmi.domain.report.web.dto.response.ReportResponseDTO;
import org.springframework.stereotype.Component;

@Component
public class ReportConverter {

    public ReportListDTO toListDTO(Report report, String targetTitle) {
        return ReportListDTO.builder()
                .nickname(report.getReporter() != null ? report.getReporter().getNickname() : null)
                .reportId(report.getReportId())
                .targetType(report.getTargetType())
                .targetId(report.getTargetId())
                .targetTitle(targetTitle)
                .reportType(report.getReportType())
                .reason(report.getReason())
                .status(report.getStatus())
                .answered(report.getAnswered())
                .createdAt(report.getCreatedAt())
                .resolvedAt(report.getResolvedAt())
                .build();
    }

    public ReportDetailDTO toDetailDTO(Report report, String targetTitle) {
        return ReportDetailDTO.builder()
                .nickname(report.getReporter() != null ? report.getReporter().getNickname() : null)
                .reportId(report.getReportId())
                .targetType(report.getTargetType())
                .targetId(report.getTargetId())
                .targetTitle(targetTitle)
                .reportType(report.getReportType())
                .reason(report.getReason())
                .status(report.getStatus())
                .answered(report.getAnswered())
                .adminAnswer(report.getAdminAnswer())
                .createdAt(report.getCreatedAt())
                .resolvedAt(report.getResolvedAt())
                .build();
    }

    public ReportResponseDTO toResponseDTO(Report report, String targetTitle) {
        return ReportResponseDTO.builder()
                .reportId(report.getReportId())
                .targetType(report.getTargetType())
                .targetId(report.getTargetId())
                .targetTitle(targetTitle)
                .reportType(report.getReportType())
                .reason(report.getReason())
                .status(report.getStatus())
                .answered(report.getAnswered())
                .adminAnswer(report.getAdminAnswer())
                .reporterNickname(report.getReporter() != null ? report.getReporter().getNickname() : null)
                .reporterEmail(report.getReporter() != null ? report.getReporter().getEmail() : null)
                .createdAt(report.getCreatedAt())
                .resolvedAt(report.getResolvedAt())
                .build();
    }
}
