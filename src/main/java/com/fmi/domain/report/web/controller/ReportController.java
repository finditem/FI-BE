package com.fmi.domain.report.web.controller;

import com.fmi.domain.auth.data.User;
import com.fmi.domain.auth.repository.UserRepository;
import com.fmi.domain.report.data.enums.ReportStatus;
import com.fmi.domain.report.service.ReportService;
import com.fmi.domain.report.web.dto.request.ReportCreateRequestDTO;
import com.fmi.domain.report.web.dto.response.ReportDetailDTO;
import com.fmi.domain.report.web.dto.response.ReportListDTO;
import com.fmi.global.apiPayload.ApiResponse;
import com.fmi.global.apiPayload.CursorPageResponse;
import com.fmi.global.apiPayload.code.status.ErrorStatus;
import com.fmi.global.apiPayload.exception.GeneralException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Report", description = "신고 API")
public class ReportController {
    
    private final ReportService reportService;
    private final UserRepository userRepository;
    
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
            @AuthenticationPrincipal UserDetails userDetails) {
        Long reportId = reportService.createReport(request, userDetails);
        return ApiResponse.onSuccess(reportId);
    }
    
    /**
     * 내 신고 상세 조회
     * GET /api/reports/{reportId}
     */
    @GetMapping("/{reportId}")
    @Operation(summary = "내 신고 상세 조회", description = "내가 접수한 신고의 상세 정보를 조회합니다. 본인의 신고만 조회 가능합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "신고 상세 조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "REPORT403-ACCESS_DENIED: 해당 신고를 조회할 권한이 없습니다"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "REPORT404-NOT_FOUND: 존재하지 않는 신고입니다")
    })
    public ApiResponse<ReportDetailDTO> getMyReportDetail(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long reportId) {

        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new GeneralException(ErrorStatus._USER_NOT_FOUND));
        ReportDetailDTO detail = reportService.getMyReportDetail(user, reportId);
        return ApiResponse.onSuccess(detail);
    }

    /**
     * 내 신고 내역 조회
     * GET /api/reports/me?status=PENDING&cursor=10&size=10
     */
    @GetMapping("/me")
    @Operation(summary = "내 신고 내역 조회", description = "커서 기반 페이지네이션으로 내가 접수한 신고 내역을 조회합니다. cursor를 생략하면 최신 항목부터 반환합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "신고 내역 조회 성공")
    })
    public ApiResponse<CursorPageResponse<ReportListDTO>> getMyReports(
            @AuthenticationPrincipal UserDetails userDetails,
            @Parameter(description = "신고 상태 필터 (PENDING=접수, REVIEWED=처리중, RESOLVED=처리완료)")
            @RequestParam(required = false) ReportStatus status,
            @Parameter(description = "답변 여부 필터 (true=답변완료, false=미답변)")
            @RequestParam(required = false) Boolean answered,
            @Parameter(description = "키워드 검색 (상세사유 또는 신고 타입 enum value, 예: SPAM)")
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "10") int size) {

        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new GeneralException(ErrorStatus._USER_NOT_FOUND));
        CursorPageResponse<ReportListDTO> reports = reportService.getMyReportsCursor(user, status, answered, keyword, cursor, size);
        return ApiResponse.onSuccess(reports);
    }
}

