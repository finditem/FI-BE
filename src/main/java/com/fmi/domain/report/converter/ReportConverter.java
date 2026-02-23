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
                .reportId(report.getReportId())
                .targetType(report.getTargetType())
                .targetId(report.getTargetId())
                .targetTitle(targetTitle)
                .reportType(report.getReportType())
                .reason(report.getReason())
                .status(report.getStatus())
                .createdAt(report.getCreatedAt())
                .resolvedAt(report.getResolvedAt())
                .build();
    }
    
    public ReportDetailDTO toDetailDTO(Report report, String targetTitle) {
        return ReportDetailDTO.builder()
                .reportId(report.getReportId())
                .targetType(report.getTargetType())
                .targetId(report.getTargetId())
                .targetTitle(targetTitle)
                .reportType(report.getReportType())
                .reason(report.getReason())
                .status(report.getStatus())
                .adminNote(report.getAdminNote())
                .answered(report.getAnswered())
                .createdAt(report.getCreatedAt())
                .resolvedAt(report.getResolvedAt())
                .build();
    }

    public ReportResponseDTO toResponseDTO(Report report) {
        return ReportResponseDTO.builder()
                .reportId(report.getReportId())
                .targetType(report.getTargetType())
                .targetId(report.getTargetId())
                .reportType(report.getReportType())
                .reason(report.getReason())
                .status(report.getStatus())
                .adminNote(report.getAdminNote())
                .createdAt(report.getCreatedAt())
                .resolvedAt(report.getResolvedAt())
                .build();
    }
}

