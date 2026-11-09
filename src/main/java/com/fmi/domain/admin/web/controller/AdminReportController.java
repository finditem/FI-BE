package com.fmi.domain.admin.web.controller;

import com.fmi.domain.admin.service.AdminReportQueryService;
import com.fmi.domain.admin.web.dto.response.AdminReportResponse;
import com.fmi.domain.admin.web.swagger.AdminReportSwagger;
import com.fmi.domain.report.data.enums.ReportStatus;
import com.fmi.domain.report.data.enums.ReportTargetType;
import com.fmi.domain.report.service.ReportService;
import com.fmi.domain.report.web.dto.request.ReportAnswerRequest;
import com.fmi.domain.report.web.dto.request.ReportStatusUpdateRequest;
import com.fmi.domain.report.web.dto.response.ReportResponse;
import com.fmi.global.apiPayload.ApiResponse;
import com.fmi.global.apiPayload.CursorPageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminReportController implements AdminReportSwagger {

    private final AdminReportQueryService adminReportQueryService;

    private final ReportService reportService;

    @GetMapping("/reports")
    @Override
    public ApiResponse<CursorPageResponse<AdminReportResponse>> getReports(
            @RequestParam(required = false) ReportStatus status,
            @RequestParam(required = false) ReportTargetType targetType,
            @RequestParam(required = false) Boolean answered,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "20") int size) {
        CursorPageResponse<AdminReportResponse> response =
                adminReportQueryService.getReportCursorPage(status, targetType, answered, keyword, cursor, size);
        return ApiResponse.onSuccess(response);
    }

    @GetMapping("/reports/{reportId}")
    @Override
    public ApiResponse<ReportResponse> getReportDetail(@PathVariable Long reportId) {
        ReportResponse response = adminReportQueryService.getReportDetail(reportId);
        return ApiResponse.onSuccess(response);
    }

    @PutMapping("/reports/{reportId}/status")
    @Override
    public ApiResponse<String> updateReportStatus(
            @PathVariable Long reportId, @Valid @RequestBody ReportStatusUpdateRequest request) {
        reportService.updateStatus(reportId, request.getStatus());
        return ApiResponse.onSuccess("OK");
    }

    @PutMapping("/reports/{reportId}/answer")
    @Override
    public ApiResponse<String> answerReport(
            @PathVariable Long reportId,
            @Valid @RequestBody ReportAnswerRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        reportService.answerReport(reportId, request.getAdminAnswer(), request.getImages(), userDetails);
        return ApiResponse.onSuccess("OK");
    }
}
