package com.fmi.domain.admin.web.controller;

import com.fmi.domain.admin.dto.AdminDeletedUserResponse;
import com.fmi.domain.admin.dto.AdminInquiryResponse;
import com.fmi.domain.admin.dto.AdminReportResponse;
import com.fmi.domain.admin.dto.AdminUserDetailResponse;
import com.fmi.domain.admin.service.AdminService;
import com.fmi.domain.admin.web.dto.AdminIpBlockRequest;
import com.fmi.domain.admin.web.dto.AdminSignupRequest;
import com.fmi.domain.auth.converter.AuthConverter;
import com.fmi.domain.auth.response.SignupResponse;
import com.fmi.domain.auth.service.AuthService;
import com.fmi.domain.Enum.WithdrawalReason;
import com.fmi.domain.inquiry.data.enums.InquiryStatus;
import com.fmi.domain.inquiry.data.enums.InquiryType;
import com.fmi.domain.inquiry.service.InquiryService;
import com.fmi.domain.inquiry.web.dto.response.InquiryDetailDTO;
import com.fmi.domain.notice.service.NoticeService;
import com.fmi.domain.notice.web.dto.NoticeCreateRequestDTO;
import com.fmi.domain.notice.web.dto.NoticeUpdateRequestDTO;
import com.fmi.domain.notice.web.dto.NoticeResponseDTO;
import com.fmi.domain.report.data.enums.ReportStatus;
import com.fmi.domain.report.data.enums.ReportTargetType;
import com.fmi.domain.report.service.ReportService;
import com.fmi.domain.report.web.dto.ReportStatusUpdateRequestDTO;
import com.fmi.domain.report.web.dto.response.ReportResponseDTO;
import com.fmi.global.apiPayload.ApiResponse;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.DeleteMapping;
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
    @Operation(summary = "관리자 문의 목록 조회", description = "문의 타입/상태 조건으로 전체 문의 내역을 조회합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "문의 목록 조회 성공")
    })
    public ApiResponse<Page<AdminInquiryResponse>> getInquiries(
            @RequestParam(required = false) InquiryType type,
            @RequestParam(required = false) InquiryStatus status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<AdminInquiryResponse> response = adminService.getInquiryPage(type, status, keyword, pageable);
        return ApiResponse.onSuccess(response);
    }

    @GetMapping("/inquiries/{inquiryId}")
    @Operation(summary = "관리자 문의 상세 조회", description = "문의 상세 정보를 조회합니다. 관리자는 비공개 문의도 조회 가능합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "문의 상세 조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "INQUIRY404-NOT_FOUND: 존재하지 않는 문의입니다",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = "{\"isSuccess\": false, \"code\": \"INQUIRY404-NOT_FOUND\", \"message\": \"존재하지 않는 문의입니다.\"}"
                            )
                    )
            )
    })
    public ApiResponse<InquiryDetailDTO> getInquiryDetail(@PathVariable Long inquiryId,
                                                          @AuthenticationPrincipal UserDetails userDetails) {
        InquiryDetailDTO response = adminService.getInquiryDetail(inquiryId, userDetails);
        return ApiResponse.onSuccess(response);
    }

    @GetMapping("/reports")
    @Operation(summary = "관리자 신고 내역 조회", description = "신고 상태/대상 유형 기준으로 신고 내역을 조회합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "신고 내역 조회 성공")
    })
    public ApiResponse<Page<AdminReportResponse>> getReports(
            @RequestParam(required = false) ReportStatus status,
            @RequestParam(required = false) ReportTargetType targetType,
            @RequestParam(required = false) Boolean answered,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<AdminReportResponse> response = adminService.getReportPage(status, targetType, answered, keyword, pageable);
        return ApiResponse.onSuccess(response);
    }

    @GetMapping("/reports/{reportId}")
    @Operation(summary = "관리자 신고 상세 조회", description = "신고 상세 정보를 조회합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "신고 상세 조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "REPORT404-NOT_FOUND: 존재하지 않는 신고입니다",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = "{\"isSuccess\": false, \"code\": \"REPORT404-NOT_FOUND\", \"message\": \"존재하지 않는 신고입니다.\"}"
                            )
                    )
            )
    })
    public ApiResponse<ReportResponseDTO> getReportDetail(@PathVariable Long reportId) {
        ReportResponseDTO response = adminService.getReportDetail(reportId);
        return ApiResponse.onSuccess(response);
    }

    @GetMapping("/users/{userId}")
    @Operation(summary = "관리자 사용자 상세 조회", description = "특정 사용자의 기본 정보 및 활동 통계를 조회합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "사용자 상세 조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "USER404-NOT_FOUND: 존재하지 않는 회원입니다",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = "{\"isSuccess\": false, \"code\": \"USER404-NOT_FOUND\", \"message\": \"존재하지 않는 회원입니다.\"}"
                            )
                    )
            )
    })
    public ApiResponse<AdminUserDetailResponse> getUserDetail(@PathVariable Long userId) {
        AdminUserDetailResponse response = adminService.getUserDetail(userId);
        return ApiResponse.onSuccess(response);
    }

    @PostMapping("/notices")
    @Operation(summary = "공지 생성(관리자)")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "공지 생성 성공")
    })
    public ApiResponse<Long> createNotice(@RequestBody NoticeCreateRequestDTO request) {
        Long id = noticeService.createNotice(request);
        return ApiResponse.onSuccess(id);
    }

    @GetMapping("/notices/drafts")
    @Operation(summary = "임시저장 공지사항 목록 조회(관리자)")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "임시저장 목록 조회 성공")
    })
    public ApiResponse<Page<com.fmi.domain.notice.web.dto.NoticeListDTO>> getDraftNotices(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<com.fmi.domain.notice.web.dto.NoticeListDTO> response = noticeService.getDraftNotices(pageable);
        return ApiResponse.onSuccess(response);
    }

    @PutMapping("/notices/{noticeId}")
    @Operation(summary = "공지 수정(관리자)")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "공지 수정 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = "{\"isSuccess\": true, \"code\": \"COMMON200\", \"message\": \"성공\", \"result\": {\"noticeId\": 1, \"title\": \"수정 제목\", \"content\": \"수정 내용\", \"category\": \"GENERAL\", \"pinned\": false, \"viewCount\": 10, \"createdAt\": \"2024-01-01T00:00:00\", \"updatedAt\": \"2024-01-02T00:00:00\"}}"
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "NOTICE404-NOT_FOUND: 존재하지 않는 공지사항입니다",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = "{\"isSuccess\": false, \"code\": \"NOTICE404-NOT_FOUND\", \"message\": \"존재하지 않는 공지사항입니다.\"}"
                            )
                    )
            )
    })
    public ApiResponse<NoticeResponseDTO> updateNotice(@PathVariable Long noticeId,
                                                       @Valid @RequestBody NoticeUpdateRequestDTO request) {
        NoticeResponseDTO response = noticeService.updateNotice(noticeId, request);
        return ApiResponse.onSuccess(response);
    }

    @DeleteMapping("/notices/{noticeId}")
    @Operation(summary = "공지 삭제(관리자)")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "공지 삭제 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = "{\"isSuccess\": true, \"code\": \"COMMON200\", \"message\": \"성공\", \"result\": \"공지사항 삭제 완료\"}"
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "NOTICE404-NOT_FOUND: 존재하지 않는 공지사항입니다",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = "{\"isSuccess\": false, \"code\": \"NOTICE404-NOT_FOUND\", \"message\": \"존재하지 않는 공지사항입니다.\"}"
                            )
                    )
            )
    })
    public ApiResponse<String> deleteNotice(@PathVariable Long noticeId) {
        noticeService.deleteNotice(noticeId);
        return ApiResponse.onSuccess("공지사항 삭제 완료");
    }

    @PutMapping("/inquiries/{inquiryId}/status")
    @Operation(summary = "문의 처리 상태 변경(관리자)")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "문의 처리 상태 변경 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "INQUIRY404-NOT_FOUND: 존재하지 않는 문의입니다",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = "{\"isSuccess\": false, \"code\": \"INQUIRY404-NOT_FOUND\", \"message\": \"존재하지 않는 문의입니다.\"}"
                            )
                    )
            )
    })
    public ApiResponse<String> updateInquiryStatus(@PathVariable Long inquiryId,
                                                   @Valid @RequestBody com.fmi.domain.inquiry.web.dto.request.InquiryStatusUpdateRequestDTO request) {
        inquiryService.updateStatus(inquiryId, request.getStatus());
        return ApiResponse.onSuccess("OK");
    }

    @PostMapping("/inquiries/{inquiryId}/block-ip")
    @Operation(summary = "문의 IP 차단(관리자)", description = "문의에 저장된 IP를 블랙리스트에 등록합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "IP 차단 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "INQUIRY404-NOT_FOUND: 존재하지 않는 문의입니다",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = "{\"isSuccess\": false, \"code\": \"INQUIRY404-NOT_FOUND\", \"message\": \"존재하지 않는 문의입니다.\"}"
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "INQUIRY400-IP_NOT_FOUND: 문의에 IP 정보가 없습니다",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = "{\"isSuccess\": false, \"code\": \"INQUIRY400-IP_NOT_FOUND\", \"message\": \"문의에 IP 정보가 없습니다.\"}"
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "INQUIRY409-IP_ALREADY_BLOCKED: 이미 차단된 IP입니다",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = "{\"isSuccess\": false, \"code\": \"INQUIRY409-IP_ALREADY_BLOCKED\", \"message\": \"이미 차단된 IP입니다.\"}"
                            )
                    )
            )
    })
    public ApiResponse<String> blockInquiryIp(@PathVariable Long inquiryId,
                                              @RequestBody(required = false) AdminIpBlockRequest request) {
        String reason = request != null ? request.getReason() : null;
        adminService.blockInquiryIp(inquiryId, reason);
        return ApiResponse.onSuccess("OK");
    }

    @PutMapping("/reports/{reportId}/status")
    @Operation(summary = "신고 처리 상태 변경(관리자)")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "신고 처리 상태 변경 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "REPORT404-NOT_FOUND: 존재하지 않는 신고입니다",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = "{\"isSuccess\": false, \"code\": \"REPORT404-NOT_FOUND\", \"message\": \"존재하지 않는 신고입니다.\"}"
                            )
                    )
            )
    })
    public ApiResponse<String> updateReportStatus(@PathVariable Long reportId,
                                                  @Valid @RequestBody ReportStatusUpdateRequestDTO request) {
        reportService.updateStatus(reportId, request.getStatus(), request.getAdminNote(), request.getAnswered());
        return ApiResponse.onSuccess("OK");
    }

    @PostMapping("/users/signup")
    @Operation(summary = "관리자 회원가입", description = "관리자 계정을 생성합니다. Role은 자동으로 ADMIN으로 설정됩니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "관리자 회원가입 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "AUTH400-WEAK_PASSWORD: 비밀번호 규칙을 만족하지 않습니다",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = "{\"isSuccess\": false, \"code\": \"AUTH400-WEAK_PASSWORD\", \"message\": \"비밀번호 규칙을 만족하지 않습니다. 8~16자, 대/소문자·숫자·특수문자를 포함해야 합니다.\"}"
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "AUTH409-EMAIL_DUPLICATED: 이미 사용 중인 이메일입니다",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = "{\"isSuccess\": false, \"code\": \"AUTH409-EMAIL_DUPLICATED\", \"message\": \"이미 사용 중인 이메일입니다.\"}"
                            )
                    )
            )
    })
    public ApiResponse<SignupResponse> adminSignup(@Valid @RequestBody AdminSignupRequest request) {
        Long id = authService.adminSignup(request);
        return ApiResponse.onSuccess(AuthConverter.toSignupResponse(id));
    }

    @GetMapping("/users/deleted")
    @Operation(summary = "탈퇴 유저 목록 조회", description = "탈퇴한 사용자 목록을 조회합니다. 탈퇴 사유로 필터링할 수 있습니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "탈퇴 유저 목록 조회 성공")
    })
    public ApiResponse<Page<AdminDeletedUserResponse>> getDeletedUsers(
            @RequestParam(required = false) WithdrawalReason reason,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "deletedAt"));
        Page<AdminDeletedUserResponse> response = adminService.getDeletedUsers(reason, pageable);
        return ApiResponse.onSuccess(response);
    }
}

