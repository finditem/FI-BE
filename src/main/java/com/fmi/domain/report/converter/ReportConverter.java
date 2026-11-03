package com.fmi.domain.report.converter;

import com.fmi.domain.report.data.Report;
import com.fmi.domain.report.web.dto.response.ReportDetailResponse;
import com.fmi.domain.report.web.dto.response.ReportListResponse;
import com.fmi.domain.report.web.dto.response.ReportResponse;
import com.fmi.domain.user.data.User;
import java.util.Collections;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class ReportConverter {

    public ReportListResponse toListDTO(Report report, String targetTitle) {
        return ReportListResponse.builder()
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

    public ReportDetailResponse toDetailDTO(Report report, String targetTitle) {
        User admin = report.getAdmin();
        return ReportDetailResponse.builder()
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
                .adminNickname(admin != null ? admin.getNickname() : null)
                .adminProfileImg(admin != null ? admin.getProfile_img() : null)
                .createdAt(report.getCreatedAt())
                .resolvedAt(report.getResolvedAt())
                .build();
    }

    public ReportResponse toResponseDTO(Report report, String targetTitle, List<String> answerImageUrls) {
        User admin = report.getAdmin();
        return ReportResponse.builder()
                .reportId(report.getReportId())
                .targetType(report.getTargetType())
                .targetId(report.getTargetId())
                .targetTitle(targetTitle)
                .reportType(report.getReportType())
                .reason(report.getReason())
                .status(report.getStatus())
                .answered(report.getAnswered())
                .adminAnswer(report.getAdminAnswer())
                .adminId(admin != null ? admin.getId() : null)
                .adminNickname(admin != null ? admin.getNickname() : null)
                .adminProfileImg(admin != null ? admin.getProfile_img() : null)
                .answerImageList(answerImageUrls != null ? answerImageUrls : Collections.emptyList())
                .answeredAt(report.getAnsweredAt())
                .reporterNickname(
                        report.getReporter() != null ? report.getReporter().getNickname() : null)
                .reporterEmail(
                        report.getReporter() != null ? report.getReporter().getEmail() : null)
                .createdAt(report.getCreatedAt())
                .resolvedAt(report.getResolvedAt())
                .build();
    }
}
