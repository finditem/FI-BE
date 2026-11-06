package com.fmi.domain.admin.web.controller;

import com.fmi.domain.admin.service.AdminReportQueryService;
import com.fmi.domain.admin.web.dto.response.AdminReportResponse;
import com.fmi.domain.report.data.enums.ReportStatus;
import com.fmi.domain.report.data.enums.ReportTargetType;
import com.fmi.domain.report.service.ReportService;
import com.fmi.domain.report.web.dto.request.ReportAnswerRequest;
import com.fmi.domain.report.web.dto.request.ReportStatusUpdateRequest;
import com.fmi.domain.report.web.dto.response.ReportResponse;
import com.fmi.global.apiPayload.ApiResponse;
import com.fmi.global.apiPayload.CursorPageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Admin", description = "관리자 전용 API")
public class AdminReportController {

    private final AdminReportQueryService adminReportQueryService;

    private final ReportService reportService;

    @GetMapping("/reports")
    @Operation(summary = "관리자 신고 내역 조회", description = "커서 기반 페이지네이션으로 신고 내역을 조회합니다. cursor를 생략하면 최신 항목부터 반환합니다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "신고 내역 조회 성공")
    })
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
    @Operation(summary = "관리자 신고 상세 조회", description = "신고 상세 정보를 조회합니다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "신고 상세 조회 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "REPORT404-NOT_FOUND: 존재하지 않는 신고입니다",
                content =
                        @Content(
                                mediaType = "application/json",
                                examples =
                                        @ExampleObject(
                                                value =
                                                        "{\"isSuccess\": false, \"code\": \"REPORT404-NOT_FOUND\", \"message\": \"존재하지 않는 신고입니다.\"}")))
    })
    public ApiResponse<ReportResponse> getReportDetail(@PathVariable Long reportId) {
        ReportResponse response = adminReportQueryService.getReportDetail(reportId);
        return ApiResponse.onSuccess(response);
    }

    @PutMapping("/reports/{reportId}/status")
    @Operation(summary = "신고 처리 상태 변경(관리자)")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "신고 처리 상태 변경 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "REPORT404-NOT_FOUND: 존재하지 않는 신고입니다",
                content =
                        @Content(
                                mediaType = "application/json",
                                examples =
                                        @ExampleObject(
                                                value =
                                                        "{\"isSuccess\": false, \"code\": \"REPORT404-NOT_FOUND\", \"message\": \"존재하지 않는 신고입니다.\"}")))
    })
    public ApiResponse<String> updateReportStatus(
            @PathVariable Long reportId, @Valid @RequestBody ReportStatusUpdateRequest request) {
        reportService.updateStatus(reportId, request.getStatus());
        return ApiResponse.onSuccess("OK");
    }

    @PutMapping("/reports/{reportId}/answer")
    @Operation(summary = "신고 답변 작성(관리자)", description = "신고에 대한 답변을 작성합니다. answered 상태가 자동으로 true로 설정됩니다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "답변 작성 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "REPORT404-NOT_FOUND: 존재하지 않는 신고입니다",
                content =
                        @Content(
                                mediaType = "application/json",
                                examples =
                                        @ExampleObject(
                                                value =
                                                        "{\"isSuccess\": false, \"code\": \"REPORT404-NOT_FOUND\", \"message\": \"존재하지 않는 신고입니다.\"}")))
    })
    public ApiResponse<String> answerReport(
            @PathVariable Long reportId,
            @Valid @RequestBody ReportAnswerRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        reportService.answerReport(reportId, request.getAdminAnswer(), request.getImages(), userDetails);
        return ApiResponse.onSuccess("OK");
    }
}
