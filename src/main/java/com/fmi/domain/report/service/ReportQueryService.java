package com.fmi.domain.report.service;

import com.fmi.domain.report.converter.ReportConverter;
import com.fmi.domain.report.data.Report;
import com.fmi.domain.report.data.enums.ReportStatus;
import com.fmi.domain.report.data.enums.ReportType;
import com.fmi.domain.report.repository.ReportRepository;
import com.fmi.domain.report.web.dto.response.ReportDetailResponse;
import com.fmi.domain.report.web.dto.response.ReportListResponse;
import com.fmi.domain.user.user.data.User;
import com.fmi.global.apiPayload.CursorPageResponse;
import com.fmi.global.apiPayload.code.status.ErrorStatus;
import com.fmi.global.apiPayload.exception.GeneralException;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportQueryService {
    private final ReportService reportService;

    private final ReportRepository reportRepository;

    private final ReportConverter reportConverter;

    /**
     * 내 신고 내역 조회
     */
    public Page<ReportListResponse> getMyReports(User user, ReportStatus status, Boolean answered, Pageable pageable) {
        Page<Report> reports;

        if (status != null && answered != null) {
            reports = reportRepository.findByReporterAndStatusAndAnswered(user, status, answered, pageable);
        } else if (status != null) {
            reports = reportRepository.findByReporterAndStatus(user, status, pageable);
        } else if (answered != null) {
            reports = reportRepository.findByReporterAndAnswered(user, answered, pageable);
        } else {
            reports = reportRepository.findByReporter(user, pageable);
        }

        return reports.map(report -> {
            String targetTitle = reportService.getTargetTitle(report.getTargetType(), report.getTargetId());
            return reportConverter.toListDTO(report, targetTitle);
        });
    }

    /**
     * 내 신고 내역 조회 (커서 기반)
     */
    public CursorPageResponse<ReportListResponse> getMyReportsCursor(
            User user, ReportStatus status, Boolean answered, String keyword, Long cursor, int size) {
        int fetchSize = size + 1;
        Pageable limit = PageRequest.of(0, fetchSize);
        List<Report> reports;

        if (keyword != null && !keyword.isBlank()) {
            String trimmed = keyword.trim();
            String lower = trimmed.toLowerCase();
            List<String> matchedTypes = Arrays.stream(ReportType.values())
                    .filter(t -> t.name().toLowerCase().contains(lower)
                            || t.getDescription().toLowerCase().contains(lower))
                    .map(Enum::name)
                    .toList();
            if (matchedTypes.isEmpty()) {
                matchedTypes = List.of("__NONE__");
            }
            reports = reportRepository.findByReporterWithKeywordCursor(
                    user.getId(),
                    status != null ? status.name() : null,
                    answered,
                    trimmed,
                    matchedTypes,
                    cursor,
                    fetchSize);
        } else if (status != null && answered != null) {
            reports = reportRepository.findByReporterAndStatusAndAnsweredCursor(user, status, answered, cursor, limit);
        } else if (status != null) {
            reports = reportRepository.findByReporterAndStatusCursor(user, status, cursor, limit);
        } else if (answered != null) {
            reports = reportRepository.findByReporterAndAnsweredCursor(user, answered, cursor, limit);
        } else {
            reports = reportRepository.findByReporterCursor(user, cursor, limit);
        }

        boolean hasNext = reports.size() > size;
        List<Report> content = hasNext ? reports.subList(0, size) : reports;
        Long nextCursor = hasNext ? content.get(content.size() - 1).getReportId() : null;

        List<ReportListResponse> responseList = content.stream()
                .map(report -> {
                    String targetTitle = reportService.getTargetTitle(report.getTargetType(), report.getTargetId());
                    return reportConverter.toListDTO(report, targetTitle);
                })
                .toList();

        return new CursorPageResponse<>(responseList, nextCursor, hasNext);
    }

    /**
     * 내 신고 상세 조회
     */
    public ReportDetailResponse getMyReportDetail(User user, Long reportId) {
        Report report = reportRepository
                .findById(reportId)
                .orElseThrow(() -> new GeneralException(ErrorStatus._REPORT_NOT_FOUND));

        if (!report.getReporter().getId().equals(user.getId())) {
            throw new GeneralException(ErrorStatus._REPORT_ACCESS_DENIED);
        }

        String targetTitle = reportService.getTargetTitle(report.getTargetType(), report.getTargetId());
        return reportConverter.toDetailDTO(report, targetTitle);
    }
}
