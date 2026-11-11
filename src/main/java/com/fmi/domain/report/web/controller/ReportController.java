package com.fmi.domain.report.web.controller;

import com.fmi.domain.report.data.enums.ReportStatus;
import com.fmi.domain.report.service.ReportQueryService;
import com.fmi.domain.report.service.ReportService;
import com.fmi.domain.report.web.dto.request.ReportCreateRequest;
import com.fmi.domain.report.web.dto.response.ReportDetailResponse;
import com.fmi.domain.report.web.dto.response.ReportListResponse;
import com.fmi.domain.report.web.swagger.ReportSwagger;
import com.fmi.domain.user.user.data.User;
import com.fmi.domain.user.user.service.UserQueryService;
import com.fmi.global.apiPayload.ApiResponse;
import com.fmi.global.apiPayload.CursorPageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/reports")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class ReportController implements ReportSwagger {
    private final ReportQueryService reportQueryService;

    private final ReportService reportService;
    private final UserQueryService userQueryService;

    /**
     * 신고하기 (통합)
     * POST /api/reports
     *
     * Request Body 예시:
     * {
     *   "targetType": "POST",      // POST, COMMENT, USER, CHAT
     *   "targetId": 123,
     *   "reportType": "FRAUD",     // FRAUD, SPAM, INAPPROPRIATE, ABUSE, etc
     *   "reason": "신고 사유 상세 설명..."
     * }
     */
    @PostMapping
    @Override
    public ApiResponse<Long> createReport(
            @Valid @RequestBody ReportCreateRequest request, @AuthenticationPrincipal UserDetails userDetails) {
        Long reportId = reportService.createReport(request, userDetails);
        return ApiResponse.onSuccess(reportId);
    }

    /**
     * 내 신고 상세 조회
     * GET /api/reports/{reportId}
     */
    @GetMapping("/{reportId}")
    @Override
    public ApiResponse<ReportDetailResponse> getMyReportDetail(
            @AuthenticationPrincipal UserDetails userDetails, @PathVariable Long reportId) {

        User user = userQueryService.findUser(userDetails.getUsername());
        ReportDetailResponse detail = reportQueryService.getMyReportDetail(user, reportId);
        return ApiResponse.onSuccess(detail);
    }

    /**
     * 내 신고 내역 조회
     * GET /api/reports/me?status=PENDING&cursor=10&size=10
     */
    @GetMapping("/me")
    @Override
    public ApiResponse<CursorPageResponse<ReportListResponse>> getMyReports(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) ReportStatus status,
            @RequestParam(required = false) Boolean answered,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "10") int size) {

        User user = userQueryService.findUser(userDetails.getUsername());
        CursorPageResponse<ReportListResponse> reports =
                reportQueryService.getMyReportsCursor(user, status, answered, keyword, cursor, size);
        return ApiResponse.onSuccess(reports);
    }
}
