package com.fmi.domain.admin.web.controller;

import com.fmi.domain.admin.dto.AdminInquiryResponse;
import com.fmi.domain.admin.dto.AdminReportResponse;
import com.fmi.domain.admin.dto.AdminUserDetailResponse;
import com.fmi.domain.admin.service.AdminService;
import com.fmi.domain.admin.web.dto.AdminSignupRequest;
import com.fmi.domain.auth.converter.AuthConverter;
import com.fmi.domain.auth.response.SignupResponse;
import com.fmi.domain.auth.service.AuthService;
import com.fmi.domain.inquiry.data.enums.InquiryCategory;
import com.fmi.domain.inquiry.data.enums.InquiryStatus;
import com.fmi.domain.inquiry.data.enums.InquiryType;
import com.fmi.domain.inquiry.service.InquiryService;
import com.fmi.domain.inquiry.web.dto.InquiryReplyCreateRequestDTO;
import com.fmi.domain.notice.service.NoticeService;
import com.fmi.domain.notice.web.dto.NoticeCreateRequestDTO;
import com.fmi.domain.report.data.enums.ReportStatus;
import com.fmi.domain.report.data.enums.ReportTargetType;
import com.fmi.domain.report.service.ReportService;
import com.fmi.domain.report.web.dto.ReportStatusUpdateRequestDTO;
import com.fmi.global.apiPayload.ApiResponse;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@Tag(name = "Admin", description = "관리자 전용 API")
public class AdminController {

    private final AdminService adminService;
    private final NoticeService noticeService;
    private final InquiryService inquiryService;
    private final ReportService reportService;
    private final AuthService authService;

    @GetMapping("/inquiries")
    @Operation(summary = "관리자 문의 목록 조회", description = "문의 유형/상태/카테고리 조건으로 전체 문의 내역을 조회합니다.")
    public ApiResponse<Page<AdminInquiryResponse>> getInquiries(
            @RequestParam(required = false) InquiryType type,
            @RequestParam(required = false) InquiryStatus status,
            @RequestParam(required = false) InquiryCategory category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<AdminInquiryResponse> response = adminService.getInquiryPage(type, status, category, pageable);
        return ApiResponse.onSuccess(response);
    }

    @GetMapping("/reports")
    @Operation(summary = "관리자 신고 내역 조회", description = "신고 상태/대상 유형 기준으로 신고 내역을 조회합니다.")
    public ApiResponse<Page<AdminReportResponse>> getReports(
            @RequestParam(required = false) ReportStatus status,
            @RequestParam(required = false) ReportTargetType targetType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<AdminReportResponse> response = adminService.getReportPage(status, targetType, pageable);
        return ApiResponse.onSuccess(response);
    }

    @GetMapping("/users/{userId}")
    @Operation(summary = "관리자 사용자 상세 조회", description = "특정 사용자의 기본 정보 및 활동 통계를 조회합니다.")
    public ApiResponse<AdminUserDetailResponse> getUserDetail(@PathVariable Long userId) {
        AdminUserDetailResponse response = adminService.getUserDetail(userId);
        return ApiResponse.onSuccess(response);
    }

    @PostMapping("/notice")
    @Operation(summary = "공지 생성(관리자)")
    public ApiResponse<Long> createNotice(@RequestBody NoticeCreateRequestDTO request) {
        Long id = noticeService.createNotice(request);
        return ApiResponse.onSuccess(id);
    }

    @PostMapping("/inquiry/{inquiryId}/reply")
    @Operation(summary = "문의 답변 등록(관리자)")
    public ApiResponse<Long> addInquiryReply(@PathVariable Long inquiryId,
                                             @RequestBody InquiryReplyCreateRequestDTO request) {
        Long replyId = inquiryService.addReply(inquiryId, request.getContent(), null);
        return ApiResponse.onSuccess(replyId);
    }

    @PutMapping("/report/{reportId}/status")
    @Operation(summary = "신고 처리 상태 변경(관리자)")
    public ApiResponse<String> updateReportStatus(@PathVariable Long reportId,
                                                  @RequestBody ReportStatusUpdateRequestDTO request) {
        reportService.updateStatus(reportId, request.getStatus(), request.getAdminNote());
        return ApiResponse.onSuccess("OK");
    }

    @PostMapping("/users/signup")
    @Operation(summary = "관리자 회원가입", description = "관리자 계정을 생성합니다. Role은 자동으로 ADMIN으로 설정됩니다.")
    public ApiResponse<SignupResponse> adminSignup(@Valid @RequestBody AdminSignupRequest request) {
        Long id = authService.adminSignup(request);
        return ApiResponse.onSuccess(AuthConverter.toSignupResponse(id));
    }
}

