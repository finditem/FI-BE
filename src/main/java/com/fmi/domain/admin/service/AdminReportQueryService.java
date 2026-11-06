package com.fmi.domain.admin.service;

import com.fmi.domain.admin.web.dto.response.AdminReportResponse;
import com.fmi.domain.report.converter.ReportConverter;
import com.fmi.domain.report.data.Report;
import com.fmi.domain.report.data.ReportAnswerImage;
import com.fmi.domain.report.data.enums.ReportStatus;
import com.fmi.domain.report.data.enums.ReportTargetType;
import com.fmi.domain.report.repository.AdminReportRepository;
import com.fmi.domain.report.repository.ReportAnswerImageRepository;
import com.fmi.domain.report.repository.ReportRepository;
import com.fmi.domain.report.service.ReportService;
import com.fmi.domain.report.web.dto.response.ReportResponse;
import com.fmi.global.apiPayload.CursorPageResponse;
import com.fmi.global.apiPayload.code.status.ErrorStatus;
import com.fmi.global.apiPayload.exception.GeneralException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminReportQueryService {
    private final AdminReportRepository adminReportRepository;

    private final ReportRepository reportRepository;

    private final ReportConverter reportConverter;

    private final ReportService reportService;

    private final ReportAnswerImageRepository reportAnswerImageRepository;

    public Page<AdminReportResponse> getReportPage(
            ReportStatus status, ReportTargetType targetType, Boolean answered, String keyword, Pageable pageable) {
        Page<Report> reports;
        if (keyword != null && !keyword.isBlank()) {
            String statusStr = status != null ? status.name() : null;
            String targetTypeStr = targetType != null ? targetType.name() : null;
            Pageable unsorted = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
            reports = adminReportRepository.findAllForAdminWithKeyword(
                    statusStr, targetTypeStr, answered, sanitizeFulltextKeyword(keyword), unsorted);
        } else {
            reports = adminReportRepository.findAllForAdmin(status, targetType, answered, pageable);
        }
        return reports.map(report -> AdminReportResponse.builder()
                .reportId(report.getReportId())
                .targetType(report.getTargetType())
                .targetId(report.getTargetId())
                .reportType(report.getReportType())
                .status(report.getStatus())
                .reason(report.getReason())
                .targetTitle(reportService.getTargetTitle(report.getTargetType(), report.getTargetId()))
                .createdAt(report.getCreatedAt())
                .updatedAt(report.getUpdatedAt())
                .resolvedAt(report.getResolvedAt())
                .reporterId(report.getReporter() != null ? report.getReporter().getId() : null)
                .reporterNickname(
                        report.getReporter() != null ? report.getReporter().getNickname() : null)
                .reporterEmail(
                        report.getReporter() != null ? report.getReporter().getEmail() : null)
                .answered(report.getAnswered())
                .build());
    }

    /**
     * 신고 상세 조회 (관리자용)
     */
    public ReportResponse getReportDetail(Long reportId) {
        Report report = reportRepository
                .findById(reportId)
                .orElseThrow(() -> new GeneralException(ErrorStatus._REPORT_NOT_FOUND));

        String targetTitle = reportService.getTargetTitle(report.getTargetType(), report.getTargetId());
        List<String> answerImageUrls = reportAnswerImageRepository.findByReportReportId(reportId).stream()
                .map(ReportAnswerImage::getImageUrl)
                .toList();
        return reportConverter.toResponseDTO(report, targetTitle, answerImageUrls);
    }

    /**
     * FULLTEXT BOOLEAN MODE 특수문자 제거
     */
    private String sanitizeFulltextKeyword(String keyword) {
        String sanitized = keyword.trim().replaceAll("[+\\-*~\"()<>@]", " ").trim();
        return sanitized.isEmpty() ? null : sanitized;
    }

    public CursorPageResponse<AdminReportResponse> getReportCursorPage(
            ReportStatus status, ReportTargetType targetType, Boolean answered, String keyword, Long cursor, int size) {
        int fetchSize = size + 1;
        List<Report> reports;
        String sanitizedKeyword = (keyword != null && !keyword.isBlank()) ? sanitizeFulltextKeyword(keyword) : null;
        if (sanitizedKeyword != null) {
            String statusStr = status != null ? status.name() : null;
            String targetTypeStr = targetType != null ? targetType.name() : null;
            reports = adminReportRepository.findAllForAdminWithKeywordCursor(
                    statusStr, targetTypeStr, answered, sanitizedKeyword, cursor, fetchSize);
        } else {
            Pageable limit = PageRequest.of(0, fetchSize);
            reports = adminReportRepository.findAllForAdminCursor(status, targetType, answered, cursor, limit);
        }

        boolean hasNext = reports.size() > size;
        List<Report> content = hasNext ? reports.subList(0, size) : reports;
        Long nextCursor = hasNext ? content.get(content.size() - 1).getReportId() : null;

        List<AdminReportResponse> responseList = content.stream()
                .map(report -> AdminReportResponse.builder()
                        .reportId(report.getReportId())
                        .targetType(report.getTargetType())
                        .targetId(report.getTargetId())
                        .reportType(report.getReportType())
                        .status(report.getStatus())
                        .reason(report.getReason())
                        .targetTitle(reportService.getTargetTitle(report.getTargetType(), report.getTargetId()))
                        .createdAt(report.getCreatedAt())
                        .updatedAt(report.getUpdatedAt())
                        .resolvedAt(report.getResolvedAt())
                        .reporterId(
                                report.getReporter() != null
                                        ? report.getReporter().getId()
                                        : null)
                        .reporterNickname(
                                report.getReporter() != null
                                        ? report.getReporter().getNickname()
                                        : null)
                        .reporterEmail(
                                report.getReporter() != null
                                        ? report.getReporter().getEmail()
                                        : null)
                        .answered(report.getAnswered())
                        .build())
                .toList();

        return new CursorPageResponse<>(responseList, nextCursor, hasNext);
    }
}
