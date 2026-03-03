package com.fmi.domain.inquiry.web.controller;

import com.fmi.domain.auth.data.User;
import com.fmi.domain.auth.repository.UserRepository;
import com.fmi.domain.inquiry.data.enums.InquiryStatus;
import com.fmi.domain.inquiry.service.InquiryService;
import com.fmi.domain.inquiry.web.dto.response.InquiryDetailDTO;
import com.fmi.domain.inquiry.web.dto.response.InquiryListDTO;
import com.fmi.global.apiPayload.ApiResponse;
import com.fmi.global.apiPayload.CursorPageResponse;
import com.fmi.global.apiPayload.code.status.ErrorStatus;
import com.fmi.global.apiPayload.exception.GeneralException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/inquiries")
@RequiredArgsConstructor
@Tag(name = "Inquiry", description = "문의 API")
public class InquiryController {
    
    private final InquiryService inquiryService;
    private final UserRepository userRepository;
    
    /**
     * 1:1 개인 문의 작성
     * POST /api/inquiries
     */
    @PostMapping
    @Operation(summary = "1:1 개인 문의 작성", description = "비회원도 가능하며, 비회원인 경우 email 필수입니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "문의 작성 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "INQUIRY400-GUEST_EMAIL_REQUIRED: 비회원 문의는 이메일이 필수입니다",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = "{\"isSuccess\": false, \"code\": \"INQUIRY400-GUEST_EMAIL_REQUIRED\", \"message\": \"비회원 문의는 이메일이 필수입니다.\"}"
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "INQUIRY403-IP_BLOCKED: 차단된 IP입니다",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = "{\"isSuccess\": false, \"code\": \"INQUIRY403-IP_BLOCKED\", \"message\": \"차단된 IP입니다.\"}"
                            )
                    )
            )
    })
    public ApiResponse<Long> createInquiry(
            @Valid @RequestBody com.fmi.domain.inquiry.web.dto.request.InquiryCreateRequestDTO request,
            @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest httpServletRequest) {
        String clientIp = getClientIpAddress(httpServletRequest);
        Long inquiryId = inquiryService.createInquiry(request, userDetails, clientIp);
        return ApiResponse.onSuccess(inquiryId);
    }
    
    /**
     * 내 문의 내역 조회
     * GET /api/inquiries/me?cursor=10&size=10
     */
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "내 문의 내역 조회", description = "커서 기반 페이지네이션으로 본인의 1:1 개인 문의를 조회합니다. cursor를 생략하면 최신 항목부터 반환합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "내 문의 내역 조회 성공")
    })
    public ApiResponse<CursorPageResponse<InquiryListDTO>> getMyInquiries(
            @AuthenticationPrincipal UserDetails userDetails,
            @Parameter(description = "문의 상태 필터 (RECEIVED=접수 대기, PENDING=처리 중, ANSWERED=답변 완료)")
            @RequestParam(required = false) InquiryStatus status,
            @Parameter(description = "답변 여부 필터 (true=답변완료, false=미답변)")
            @RequestParam(required = false) Boolean answered,
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "10") int size) {

        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new GeneralException(ErrorStatus._USER_NOT_FOUND));
        CursorPageResponse<InquiryListDTO> inquiries = inquiryService.getMyInquiriesCursor(user, status, answered, cursor, size);
        return ApiResponse.onSuccess(inquiries);
    }

    /**
     * 문의 상세 조회
     * GET /api/inquiries/{inquiryId}
     */
    @GetMapping("/{inquiryId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "문의 상세 조회", description = "본인의 1:1 개인 문의만 조회 가능합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "문의 상세 조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "INQUIRY403-ACCESS_DENIED: 해당 문의를 조회할 권한이 없습니다",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = "{\"isSuccess\": false, \"code\": \"INQUIRY403-ACCESS_DENIED\", \"message\": \"해당 문의를 조회할 권한이 없습니다.\"}"
                            )
                    )
            ),
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
    public ApiResponse<InquiryDetailDTO> getInquiryDetail(
            @PathVariable Long inquiryId,
            @AuthenticationPrincipal UserDetails userDetails) {

        InquiryDetailDTO inquiry = inquiryService.getInquiryDetail(inquiryId, userDetails);
        return ApiResponse.onSuccess(inquiry);
    }

    private String getClientIpAddress(HttpServletRequest request) {
        if (request == null) {
            return "unknown";
        }
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip != null ? ip : "unknown";
    }
}

