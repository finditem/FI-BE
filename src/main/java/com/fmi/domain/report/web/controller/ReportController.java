package com.fmi.domain.report.web.controller;

import com.fmi.domain.auth.data.User;
import com.fmi.domain.report.data.enums.ReportStatus;
import com.fmi.domain.report.service.ReportService;
import com.fmi.domain.report.web.dto.request.ReportCreateRequestDTO;
import com.fmi.domain.report.web.dto.response.ReportListDTO;
import com.fmi.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/reports")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
@Tag(name = "Report", description = "신고 API")
public class ReportController {
    
    private final ReportService reportService;
    
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
    @Operation(
        summary = "신고하기", 
        description = "게시글, 댓글, 사용자, 채팅을 신고할 수 있습니다. targetType과 reportType을 조합하여 다양한 신고 유형을 처리합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "신고 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "POST404-NOT_FOUND: 존재하지 않는 게시글입니다"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "COMMENT404-NOT_FOUND: 존재하지 않는 댓글입니다"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "USER404-NOT_FOUND: 존재하지 않는 회원입니다"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "CHATROOM404-NOT_FOUND: 존재하지 않는 채팅방입니다"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "REPORT409-ALREADY_EXISTS: 이미 신고한 대상입니다")
    })
    public ApiResponse<Long> createReport(
            @Valid @RequestBody ReportCreateRequestDTO request,
            @AuthenticationPrincipal User user) {
        
        Long reportId = reportService.createReport(request, user);
        return ApiResponse.onSuccess(reportId);
    }
    
    /**
     * 내 신고 내역 조회
     * GET /api/reports/me?status=PENDING&page=0&size=10
     */
    @GetMapping("/me")
    @Operation(summary = "내 신고 내역 조회", description = "내가 접수한 모든 신고 내역을 조회합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "신고 내역 조회 성공")
    })
    public ApiResponse<Page<ReportListDTO>> getMyReports(
            @AuthenticationPrincipal User user,
            @Parameter(description = "신고 상태 필터 (PENDING=접수, REVIEWED=처리중, RESOLVED=처리완료)", required = false)
            @RequestParam(required = false) ReportStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<ReportListDTO> reports = reportService.getMyReports(user, status, pageable);
        return ApiResponse.onSuccess(reports);
    }
}

