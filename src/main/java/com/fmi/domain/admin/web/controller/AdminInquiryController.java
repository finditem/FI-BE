package com.fmi.domain.admin.web.controller;

import com.fmi.domain.admin.service.AdminInquiryService;
import com.fmi.domain.admin.web.dto.request.AdminGuestInquiryReplyRequest;
import com.fmi.domain.admin.web.dto.request.AdminIpBlockRequest;
import com.fmi.domain.admin.web.dto.response.AdminGuestInquiryPageResponse;
import com.fmi.domain.admin.web.dto.response.AdminInquiryDetailResponse;
import com.fmi.domain.admin.web.dto.response.AdminInquiryResponse;
import com.fmi.domain.admin.web.swagger.AdminInquirySwagger;
import com.fmi.domain.inquiry.inquiry.data.enums.InquiryStatus;
import com.fmi.domain.inquiry.inquiry.data.enums.InquiryType;
import com.fmi.domain.inquiry.inquiry.service.InquiryService;
import com.fmi.domain.inquiry.inquiry.web.dto.response.InquiryDetailResponse;
import com.fmi.global.apiPayload.ApiResponse;
import com.fmi.global.apiPayload.CursorPageResponse;
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
public class AdminInquiryController implements AdminInquirySwagger {

    private final AdminInquiryService adminInquiryService;

    private final InquiryService inquiryService;

    @GetMapping("/guest-inquiries/{inquiryId}")
    @Override
    public ApiResponse<InquiryDetailResponse> getGuestInquiryDetail(@PathVariable Long inquiryId) {
        InquiryDetailResponse response = adminInquiryService.getGuestInquiryDetail(inquiryId);
        return ApiResponse.onSuccess(response);
    }

    @GetMapping("/guest-inquiries")
    @Override
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
    @Override
    public ApiResponse<CursorPageResponse<AdminInquiryResponse>> getInquiries(
            @RequestParam(required = false) InquiryType type,
            @RequestParam(required = false) InquiryStatus status,
            @RequestParam(required = false) Boolean answered,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "20") int size) {
        CursorPageResponse<AdminInquiryResponse> response =
                adminInquiryService.getInquiryCursorPage(type, status, answered, keyword, cursor, size);
        return ApiResponse.onSuccess(response);
    }

    @GetMapping("/inquiries/{inquiryId}")
    @Override
    public ApiResponse<AdminInquiryDetailResponse> getInquiryDetail(
            @PathVariable Long inquiryId, @AuthenticationPrincipal UserDetails userDetails) {
        AdminInquiryDetailResponse response = adminInquiryService.getInquiryDetail(inquiryId, userDetails);
        return ApiResponse.onSuccess(response);
    }

    @PostMapping("/guest-inquiries/{inquiryId}/reply")
    @Override
    public ApiResponse<String> replyToGuestInquiry(
            @PathVariable Long inquiryId, @Valid @RequestBody AdminGuestInquiryReplyRequest request) {
        inquiryService.replyToGuestInquiry(inquiryId, request.getContent());
        return ApiResponse.onSuccess("OK");
    }

    @PutMapping("/inquiries/{inquiryId}/status")
    @Override
    public ApiResponse<String> updateInquiryStatus(
            @PathVariable Long inquiryId,
            @Valid @RequestBody com.fmi.domain.inquiry.inquiry.web.dto.request.InquiryStatusUpdateRequest request) {
        inquiryService.updateStatus(inquiryId, request.getStatus());
        return ApiResponse.onSuccess("OK");
    }

    @PostMapping("/inquiries/{inquiryId}/block-ip")
    @Override
    public ApiResponse<String> blockInquiryIp(
            @PathVariable Long inquiryId, @RequestBody(required = false) AdminIpBlockRequest request) {
        String reason = request != null ? request.getReason() : null;
        adminInquiryService.blockInquiryIp(inquiryId, reason);
        return ApiResponse.onSuccess("OK");
    }
}
