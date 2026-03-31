package com.fmi.domain.admin.web.controller;

import com.fmi.domain.Enum.Category;
import com.fmi.domain.Enum.SortType;
import com.fmi.domain.admin.dto.AdminDeletedUserResponse;
import com.fmi.domain.admin.dto.AdminGuestInquiryPageResponse;
import com.fmi.domain.admin.dto.AdminGuestInquiryReplyRequestDTO;
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
import com.fmi.domain.admin.dto.AdminInquiryDetailDTO;
import com.fmi.domain.inquiry.web.dto.response.InquiryDetailDTO;
import com.fmi.domain.notice.service.NoticeService;
import com.fmi.domain.notice.web.dto.NoticeCreateRequestDTO;
import com.fmi.domain.notice.web.dto.NoticeUpdateRequestDTO;
import com.fmi.domain.notice.web.dto.NoticeResponseDTO;
import com.fmi.domain.post.data.PostStatus;
import com.fmi.domain.post.service.PostQueryService;
import com.fmi.domain.post.web.dto.response.MarketingConsentPostPageResponse;
import com.fmi.domain.report.data.enums.ReportStatus;
import com.fmi.domain.report.data.enums.ReportTargetType;
import com.fmi.domain.report.service.ReportService;
import com.fmi.domain.report.web.dto.ReportStatusUpdateRequestDTO;
import com.fmi.domain.report.web.dto.request.ReportAnswerRequestDTO;
import com.fmi.domain.report.web.dto.response.ReportResponseDTO;
import com.fmi.global.apiPayload.ApiResponse;
import com.fmi.global.apiPayload.CursorPageResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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
    private final PostQueryService postQueryService;

    @GetMapping("/guest-inquiries/{inquiryId}")
    @Operation(summary = "관리자 비회원 문의 상세 조회", description = "비회원이 작성한 문의의 상세 정보를 조회합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "비회원 문의 상세 조회 성공"),
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
    public ApiResponse<InquiryDetailDTO> getGuestInquiryDetail(@PathVariable Long inquiryId) {
        InquiryDetailDTO response = adminService.getGuestInquiryDetail(inquiryId);
        return ApiResponse.onSuccess(response);
    }

    @GetMapping("/guest-inquiries")
    @Operation(summary = "관리자 비회원 문의 목록 조회", description = """
            비회원이 작성한 문의 내역을 조회합니다. (커서 기반 무한스크롤)

            - 첫 요청: cursor 없이 호출
            - 다음 요청: 응답의 nextCursor를 cursor 파라미터로 전달
            예) /admin/guest-inquiries?size=20 → /admin/guest-inquiries?cursor=98&size=20
            """)
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "비회원 문의 목록 조회 성공")
    })
    public ApiResponse<AdminGuestInquiryPageResponse> getGuestInquiries(
            @RequestParam(required = false) InquiryStatus status,
            @RequestParam(required = false) Boolean answered,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "20") int size
    ) {
        AdminGuestInquiryPageResponse response = adminService.getGuestInquirySlice(status, answered, keyword, cursor, size);
        return ApiResponse.onSuccess(response);
    }

    @GetMapping("/inquiries")
    @Operation(summary = "관리자 회원 문의 목록 조회", description = "커서 기반 페이지네이션으로 회원이 작성한 문의 내역을 조회합니다. (비회원 문의 제외) cursor를 생략하면 최신 항목부터 반환합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "문의 목록 조회 성공")
    })
    public ApiResponse<CursorPageResponse<AdminInquiryResponse>> getInquiries(
            @RequestParam(required = false) InquiryType type,
            @RequestParam(required = false) InquiryStatus status,
            @Parameter(description = "답변 여부 필터 (true=답변완료, false=미답변)")
            @RequestParam(required = false) Boolean answered,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "20") int size
    ) {
        CursorPageResponse<AdminInquiryResponse> response =
                adminService.getInquiryCursorPage(type, status, answered, keyword, cursor, size);
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
    public ApiResponse<AdminInquiryDetailDTO> getInquiryDetail(@PathVariable Long inquiryId,
                                                               @AuthenticationPrincipal UserDetails userDetails) {
        AdminInquiryDetailDTO response = adminService.getInquiryDetail(inquiryId, userDetails);
        return ApiResponse.onSuccess(response);
    }

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
            @RequestParam(defaultValue = "20") int size
    ) {
        CursorPageResponse<AdminReportResponse> response =
                adminService.getReportCursorPage(status, targetType, answered, keyword, cursor, size);
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
    public ApiResponse<Long> createNotice(@Valid @RequestBody NoticeCreateRequestDTO request,
                                             @AuthenticationPrincipal UserDetails userDetails) {
        Long id = noticeService.createNotice(request, userDetails.getUsername());
        return ApiResponse.onSuccess(id);
    }

    @GetMapping("/notices/draft")
    @Operation(summary = "임시저장 공지사항 조회(관리자)", description = "현재 로그인한 관리자의 임시저장 공지사항을 조회합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "임시저장 조회 성공")
    })
    public ApiResponse<NoticeResponseDTO> getDraftNotice(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        NoticeResponseDTO response = noticeService.getDraftNotice(userDetails.getUsername());
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

    @PostMapping("/guest-inquiries/{inquiryId}/reply")
    @Operation(summary = "비회원 문의 답변 (이메일 발송)", description = "비회원 문의에 대해 이메일로 답변을 발송합니다. 상태가 ANSWERED로 변경됩니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "답변 발송 성공"),
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
                    description = "회원 문의이거나 이메일이 없는 경우",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = "{\"isSuccess\": false, \"code\": \"COMMON400\", \"message\": \"잘못된 요청입니다.\"}"
                            )
                    )
            )
    })
    public ApiResponse<String> replyToGuestInquiry(@PathVariable Long inquiryId,
                                                    @Valid @RequestBody AdminGuestInquiryReplyRequestDTO request) {
        inquiryService.replyToGuestInquiry(inquiryId, request.getContent());
        return ApiResponse.onSuccess("OK");
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
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = "{\"isSuccess\": false, \"code\": \"REPORT404-NOT_FOUND\", \"message\": \"존재하지 않는 신고입니다.\"}"
                            )
                    )
            )
    })
    public ApiResponse<String> answerReport(@PathVariable Long reportId,
                                            @Valid @RequestBody ReportAnswerRequestDTO request,
                                            @AuthenticationPrincipal UserDetails userDetails) {
        reportService.answerReport(reportId, request.getAdminAnswer(), request.getImages(), userDetails);
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
    @Operation(summary = "탈퇴 유저 목록 조회", description = "커서 기반 페이지네이션으로 탈퇴한 사용자 목록을 조회합니다. cursor를 생략하면 최신 항목부터 반환합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "탈퇴 유저 목록 조회 성공")
    })
    public ApiResponse<CursorPageResponse<AdminDeletedUserResponse>> getDeletedUsers(
            @RequestParam(required = false) WithdrawalReason reason,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "20") int size
    ) {
        CursorPageResponse<AdminDeletedUserResponse> response =
                adminService.getDeletedUsersCursorPage(reason, keyword, cursor, size);
        return ApiResponse.onSuccess(response);
    }


    @GetMapping("/marketing-consent/posts")
    @Operation(
            summary = "마케팅 수신 동의 사용자 게시글 조회 (관리자 전용)",
            description = """
                    마케팅 수신에 동의한 사용자가 작성한 게시글 목록을 조회합니다.
                    
                    - 관리자만 접근할 수 있습니다.
                    - 무한스크롤 방식으로 조회합니다.
                    - 첫 요청은 cursor 없이 호출하고, 이후 응답의 nextCursor 값을 다음 요청의 cursor로 전달합니다.
                    - 정렬(sortType), 카테고리(category), 게시글 상태(postStatus), 키워드(keyword) 필터를 지원합니다.
                    - keyword는 제목(title), 내용(content)을 기준으로 검색합니다.
                    
                    예)
                    - 첫 요청:
                      /posts/marketing-consent?sortType=LATEST&size=20
                    - 다음 요청:
                      /posts/marketing-consent?sortType=LATEST&cursor=120&size=20
                    - 키워드 검색:
                      /posts/marketing-consent?keyword=지갑&sortType=LATEST
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = MarketingConsentPostPageResponse.class),
                            examples = @ExampleObject(
                                    value = """
                                            {
                                              "isSuccess": true,
                                              "code": "COMMON200",
                                              "message": "성공",
                                              "result": {
                                                "postList": [
                                                  {
                                                    "id": 101,
                                                    "title": "지갑을 습득했습니다",
                                                    "summary": "강남역 근처에서 검정색 지갑을 주웠습니다.",
                                                    "thumbnailImageUrl": "https://example.com/thumb.jpg",
                                                    "address": "서울 강남구",
                                                    "postStatus": "SEARCHING",
                                                    "postType": "FOUND",
                                                    "category": "WALLET",
                                                    "favoriteCount": 12,
                                                    "favoriteStatus": false,
                                                    "viewCount": 104,
                                                    "isNew": true,
                                                    "isHot": false,
                                                    "createdAt": "2026-04-01T12:30:00",
                                                    "imageCount": 2
                                                  },
                                                  {
                                                    "id": 99,
                                                    "title": "휴대폰을 찾습니다",
                                                    "summary": "버스 안에서 휴대폰을 잃어버렸습니다.",
                                                    "thumbnailImageUrl": null,
                                                    "address": "서울 서초구",
                                                    "postStatus": "FOUND",
                                                    "postType": "LOST",
                                                    "category": "ELECTRONICS",
                                                    "favoriteCount": 5,
                                                    "favoriteStatus": true,
                                                    "viewCount": 87,
                                                    "isNew": false,
                                                    "isHot": true,
                                                    "createdAt": "2026-03-31T18:10:00",
                                                    "imageCount": 1
                                                  }
                                                ],
                                                "nextCursor": 99,
                                                "hasNext": true
                                              }
                                            }
                                            """
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청 파라미터",
                    content = @Content
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증이 필요합니다",
                    content = @Content
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "관리자만 접근할 수 있습니다",
                    content = @Content
            )
    })
    public ResponseEntity<ApiResponse<MarketingConsentPostPageResponse>> getMarketingConsentPosts(@RequestParam(required = false) Long cursor,
                                                                                                  @RequestParam(defaultValue = "20") int size,
                                                                                                  @RequestParam(defaultValue = "LATEST") SortType sortType,
                                                                                                  @RequestParam(required = false) Category category,
                                                                                                  @RequestParam(required = false) PostStatus postStatus,
                                                                                                  @RequestParam(required = false) String keyword,
                                                                                                  @AuthenticationPrincipal UserDetails userDetails) {
        MarketingConsentPostPageResponse response = postQueryService.getMarketingConsentPosts(cursor, size, sortType, category, postStatus, keyword, userDetails);

        return ResponseEntity.ok(ApiResponse.onSuccess(response));
    }
}

