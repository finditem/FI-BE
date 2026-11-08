package com.fmi.domain.admin.web.controller;

import com.fmi.domain.admin.service.AdminInquiryService;
import com.fmi.domain.admin.web.dto.request.AdminGuestInquiryReplyRequest;
import com.fmi.domain.admin.web.dto.request.AdminIpBlockRequest;
import com.fmi.domain.admin.web.dto.response.AdminGuestInquiryPageResponse;
import com.fmi.domain.admin.web.dto.response.AdminInquiryDetailResponse;
import com.fmi.domain.admin.web.dto.response.AdminInquiryResponse;
import com.fmi.domain.inquiry.inquiry.data.enums.InquiryStatus;
import com.fmi.domain.inquiry.inquiry.data.enums.InquiryType;
import com.fmi.domain.inquiry.inquiry.service.InquiryService;
import com.fmi.domain.inquiry.inquiry.web.dto.response.InquiryDetailResponse;
import com.fmi.global.apiPayload.ApiResponse;
import com.fmi.global.apiPayload.CursorPageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
public class AdminInquiryController {

    private final AdminInquiryService adminInquiryService;

    private final InquiryService inquiryService;

    @GetMapping("/guest-inquiries/{inquiryId}")
    @Operation(summary = "관리자 비회원 문의 상세 조회", description = "비회원이 작성한 문의의 상세 정보를 조회합니다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "비회원 문의 상세 조회 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "INQUIRY404-NOT_FOUND: 존재하지 않는 문의입니다",
                content =
                        @Content(
                                mediaType = "application/json",
                                examples =
                                        @ExampleObject(
                                                value =
                                                        "{\"isSuccess\": false, \"code\": \"INQUIRY404-NOT_FOUND\", \"message\": \"존재하지 않는 문의입니다.\"}")))
    })
    public ApiResponse<InquiryDetailResponse> getGuestInquiryDetail(@PathVariable Long inquiryId) {
        InquiryDetailResponse response = adminInquiryService.getGuestInquiryDetail(inquiryId);
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
            @RequestParam(defaultValue = "20") int size) {
        AdminGuestInquiryPageResponse response =
                adminInquiryService.getGuestInquirySlice(status, answered, keyword, cursor, size);
        return ApiResponse.onSuccess(response);
    }

    @GetMapping("/inquiries")
    @Operation(
            summary = "관리자 회원 문의 목록 조회",
            description = "커서 기반 페이지네이션으로 회원이 작성한 문의 내역을 조회합니다. (비회원 문의 제외) cursor를 생략하면 최신 항목부터 반환합니다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "문의 목록 조회 성공")
    })
    public ApiResponse<CursorPageResponse<AdminInquiryResponse>> getInquiries(
            @RequestParam(required = false) InquiryType type,
            @RequestParam(required = false) InquiryStatus status,
            @Parameter(description = "답변 여부 필터 (true=답변완료, false=미답변)") @RequestParam(required = false)
                    Boolean answered,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "20") int size) {
        CursorPageResponse<AdminInquiryResponse> response =
                adminInquiryService.getInquiryCursorPage(type, status, answered, keyword, cursor, size);
        return ApiResponse.onSuccess(response);
    }

    @GetMapping("/inquiries/{inquiryId}")
    @Operation(summary = "관리자 문의 상세 조회", description = "문의 상세 정보를 조회합니다. 관리자는 비공개 문의도 조회 가능합니다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "문의 상세 조회 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "INQUIRY404-NOT_FOUND: 존재하지 않는 문의입니다",
                content =
                        @Content(
                                mediaType = "application/json",
                                examples =
                                        @ExampleObject(
                                                value =
                                                        "{\"isSuccess\": false, \"code\": \"INQUIRY404-NOT_FOUND\", \"message\": \"존재하지 않는 문의입니다.\"}")))
    })
    public ApiResponse<AdminInquiryDetailResponse> getInquiryDetail(
            @PathVariable Long inquiryId, @AuthenticationPrincipal UserDetails userDetails) {
        AdminInquiryDetailResponse response = adminInquiryService.getInquiryDetail(inquiryId, userDetails);
        return ApiResponse.onSuccess(response);
    }

    @PostMapping("/guest-inquiries/{inquiryId}/reply")
    @Operation(summary = "비회원 문의 답변 (이메일 발송)", description = "비회원 문의에 대해 이메일로 답변을 발송합니다. 상태가 ANSWERED로 변경됩니다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "답변 발송 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "INQUIRY404-NOT_FOUND: 존재하지 않는 문의입니다",
                content =
                        @Content(
                                mediaType = "application/json",
                                examples =
                                        @ExampleObject(
                                                value =
                                                        "{\"isSuccess\": false, \"code\": \"INQUIRY404-NOT_FOUND\", \"message\": \"존재하지 않는 문의입니다.\"}"))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "회원 문의이거나 이메일이 없는 경우",
                content =
                        @Content(
                                mediaType = "application/json",
                                examples =
                                        @ExampleObject(
                                                value =
                                                        "{\"isSuccess\": false, \"code\": \"COMMON400\", \"message\": \"잘못된 요청입니다.\"}")))
    })
    public ApiResponse<String> replyToGuestInquiry(
            @PathVariable Long inquiryId, @Valid @RequestBody AdminGuestInquiryReplyRequest request) {
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
                content =
                        @Content(
                                mediaType = "application/json",
                                examples =
                                        @ExampleObject(
                                                value =
                                                        "{\"isSuccess\": false, \"code\": \"INQUIRY404-NOT_FOUND\", \"message\": \"존재하지 않는 문의입니다.\"}")))
    })
    public ApiResponse<String> updateInquiryStatus(
            @PathVariable Long inquiryId,
            @Valid @RequestBody com.fmi.domain.inquiry.inquiry.web.dto.request.InquiryStatusUpdateRequest request) {
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
                content =
                        @Content(
                                mediaType = "application/json",
                                examples =
                                        @ExampleObject(
                                                value =
                                                        "{\"isSuccess\": false, \"code\": \"INQUIRY404-NOT_FOUND\", \"message\": \"존재하지 않는 문의입니다.\"}"))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "INQUIRY400-IP_NOT_FOUND: 문의에 IP 정보가 없습니다",
                content =
                        @Content(
                                mediaType = "application/json",
                                examples =
                                        @ExampleObject(
                                                value =
                                                        "{\"isSuccess\": false, \"code\": \"INQUIRY400-IP_NOT_FOUND\", \"message\": \"문의에 IP 정보가 없습니다.\"}"))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "409",
                description = "INQUIRY409-IP_ALREADY_BLOCKED: 이미 차단된 IP입니다",
                content =
                        @Content(
                                mediaType = "application/json",
                                examples =
                                        @ExampleObject(
                                                value =
                                                        "{\"isSuccess\": false, \"code\": \"INQUIRY409-IP_ALREADY_BLOCKED\", \"message\": \"이미 차단된 IP입니다.\"}")))
    })
    public ApiResponse<String> blockInquiryIp(
            @PathVariable Long inquiryId, @RequestBody(required = false) AdminIpBlockRequest request) {
        String reason = request != null ? request.getReason() : null;
        adminInquiryService.blockInquiryIp(inquiryId, reason);
        return ApiResponse.onSuccess("OK");
    }
}
