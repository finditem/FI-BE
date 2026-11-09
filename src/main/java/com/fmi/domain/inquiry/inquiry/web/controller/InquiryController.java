package com.fmi.domain.inquiry.inquiry.web.controller;

import com.fmi.domain.inquiry.inquiry.data.enums.InquiryStatus;
import com.fmi.domain.inquiry.inquiry.service.InquiryDetailService;
import com.fmi.domain.inquiry.inquiry.service.InquiryQueryService;
import com.fmi.domain.inquiry.inquiry.service.InquiryService;
import com.fmi.domain.inquiry.inquiry.web.dto.response.InquiryDetailResponse;
import com.fmi.domain.inquiry.inquiry.web.dto.response.InquiryListResponse;
import com.fmi.domain.inquiry.inquiry.web.swagger.InquirySwagger;
import com.fmi.domain.user.user.data.User;
import com.fmi.domain.user.user.service.UserQueryService;
import com.fmi.global.apiPayload.ApiResponse;
import com.fmi.global.apiPayload.CursorPageResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/inquiries")
@RequiredArgsConstructor
public class InquiryController implements InquirySwagger {
    private final InquiryDetailService inquiryDetailService;
    private final InquiryQueryService inquiryQueryService;

    private final InquiryService inquiryService;
    private final UserQueryService userQueryService;

    /**
     * 1:1 개인 문의 작성
     * POST /api/inquiries
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Override
    public ApiResponse<Long> createInquiry(
            @Valid @RequestPart("inquiry") com.fmi.domain.inquiry.inquiry.web.dto.request.InquiryCreateRequest request,
            @RequestPart(value = "images", required = false) List<MultipartFile> images,
            @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest httpServletRequest) {
        String clientIp = getClientIpAddress(httpServletRequest);
        Long inquiryId = inquiryService.createInquiry(request, userDetails, clientIp, images);
        return ApiResponse.onSuccess(inquiryId);
    }

    /**
     * 내 문의 내역 조회
     * GET /api/inquiries/me?cursor=10&size=10
     */
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    @Override
    public ApiResponse<CursorPageResponse<InquiryListResponse>> getMyInquiries(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) InquiryStatus status,
            @RequestParam(required = false) Boolean answered,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "10") int size) {

        User user = userQueryService.findUser(userDetails.getUsername());
        CursorPageResponse<InquiryListResponse> inquiries =
                inquiryQueryService.getMyInquiriesCursor(user, status, answered, keyword, cursor, size);
        return ApiResponse.onSuccess(inquiries);
    }

    /**
     * 문의 상세 조회
     * GET /api/inquiries/{inquiryId}
     */
    @GetMapping("/{inquiryId}")
    @PreAuthorize("isAuthenticated()")
    @Override
    public ApiResponse<InquiryDetailResponse> getInquiryDetail(
            @PathVariable Long inquiryId, @AuthenticationPrincipal UserDetails userDetails) {

        InquiryDetailResponse inquiry = inquiryDetailService.getInquiryDetail(inquiryId, userDetails);

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
