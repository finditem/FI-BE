package com.fmi.domain.report.web.controller;

import com.fmi.domain.report.service.ReportService;
import com.fmi.domain.report.web.dto.ReportStatusUpdateRequestDTO;
import com.fmi.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/report")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin", description = "관리자 API")
public class AdminReportController {

    private final ReportService reportService;

    @PutMapping("/{reportId}/status")
    @Operation(summary = "신고 처리 상태 변경(관리자)")
    public ApiResponse<String> updateStatus(@PathVariable Long reportId,
                                            @RequestBody ReportStatusUpdateRequestDTO request) {
        reportService.updateStatus(reportId, request.getStatus(), request.getAdminNote());
        return ApiResponse.onSuccess("OK");
    }
}


