package com.fmi.domain.admin.web.swagger;

import com.fmi.domain.admin.web.dto.response.AdminReportResponse;
import com.fmi.domain.report.data.enums.ReportStatus;
import com.fmi.domain.report.data.enums.ReportTargetType;
import com.fmi.domain.report.web.dto.request.ReportAnswerRequest;
import com.fmi.domain.report.web.dto.request.ReportStatusUpdateRequest;
import com.fmi.domain.report.web.dto.response.ReportResponse;
import com.fmi.global.apiPayload.ApiResponse;
import com.fmi.global.apiPayload.CursorPageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.userdetails.UserDetails;

@Tag(name = "Admin", description = "관리자 전용 API")
public interface AdminReportSwagger {

    @Operation(summary = "관리자 신고 내역 조회", description = "커서 기반 페이지네이션으로 신고 내역을 조회합니다. cursor를 생략하면 최신 항목부터 반환합니다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "신고 내역 조회 성공")
    })
    ApiResponse<CursorPageResponse<AdminReportResponse>> getReports(
            ReportStatus status, ReportTargetType targetType, Boolean answered, String keyword, Long cursor, int size);

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
    ApiResponse<ReportResponse> getReportDetail(Long reportId);

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
    ApiResponse<String> updateReportStatus(Long reportId, ReportStatusUpdateRequest request);

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
    ApiResponse<String> answerReport(
            Long reportId, ReportAnswerRequest request, @Parameter(hidden = true) UserDetails userDetails);
}
