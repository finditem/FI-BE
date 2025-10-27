package com.fmi.domain.inquiry.web.controller;

import com.fmi.domain.auth.data.User;
import com.fmi.domain.inquiry.data.enums.InquiryCategory;
import com.fmi.domain.inquiry.data.enums.InquiryStatus;
import com.fmi.domain.inquiry.service.InquiryService;
import com.fmi.domain.inquiry.web.dto.request.InquiryPrivateRequestDTO;
import com.fmi.domain.inquiry.web.dto.request.InquiryPublicRequestDTO;
import com.fmi.domain.inquiry.web.dto.response.InquiryDetailDTO;
import com.fmi.domain.inquiry.web.dto.response.InquiryListDTO;
import com.fmi.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
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
@RequestMapping("/api/inquiry")
@RequiredArgsConstructor
@Tag(name = "Inquiry", description = "문의 API")
public class InquiryController {
    
    private final InquiryService inquiryService;
    
    /**
     * 공개 문의 작성
     * POST /api/inquiry/public
     */
    @PostMapping("/public")
    @Operation(summary = "공개 문의 작성", description = "모든 사용자가 볼 수 있는 문의를 작성합니다. 비회원도 작성 가능합니다.")
    public ApiResponse<Long> createPublicInquiry(
            @Valid @RequestBody InquiryPublicRequestDTO request,
            @AuthenticationPrincipal User user) {
        
        Long inquiryId = inquiryService.createPublicInquiry(request, user);
        return ApiResponse.onSuccess(inquiryId);
    }
    
    /**
     * 1:1 개인 문의 작성
     * POST /api/inquiry/private
     */
    @PostMapping("/private")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "1:1 문의 작성", description = "본인과 관리자만 볼 수 있는 비공개 문의를 작성합니다.")
    public ApiResponse<Long> createPrivateInquiry(
            @Valid @RequestBody InquiryPrivateRequestDTO request,
            @AuthenticationPrincipal User user) {
        
        Long inquiryId = inquiryService.createPrivateInquiry(request, user);
        return ApiResponse.onSuccess(inquiryId);
    }
    
    /**
     * 공개 문의 목록 조회
     * GET /api/inquiry/public?category=GENERAL&status=PENDING&page=0&size=10
     */
    @GetMapping("/public")
    @Operation(summary = "공개 문의 목록 조회")
    public ApiResponse<Page<InquiryListDTO>> getPublicInquiryList(
            @RequestParam(required = false) InquiryCategory category,
            @RequestParam(required = false) InquiryStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<InquiryListDTO> inquiries = inquiryService.getPublicInquiryList(category, status, pageable);
        return ApiResponse.onSuccess(inquiries);
    }
    
    /**
     * 내 문의 내역 조회
     * GET /api/inquiry/me?page=0&size=10
     */
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "내 문의 내역 조회", description = "공개/비공개 문의를 모두 조회합니다.")
    public ApiResponse<Page<InquiryListDTO>> getMyInquiries(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<InquiryListDTO> inquiries = inquiryService.getMyInquiries(user, pageable);
        return ApiResponse.onSuccess(inquiries);
    }
    
    /**
     * 문의 상세 조회
     * GET /api/inquiry/{inquiryId}
     */
    @GetMapping("/{inquiryId}")
    @Operation(summary = "문의 상세 조회", description = "공개 문의는 누구나, 비공개 문의는 본인만 조회 가능합니다.")
    public ApiResponse<InquiryDetailDTO> getInquiryDetail(
            @PathVariable Long inquiryId,
            @AuthenticationPrincipal User user) {
        
        InquiryDetailDTO inquiry = inquiryService.getInquiryDetail(inquiryId, user);
        return ApiResponse.onSuccess(inquiry);
    }
}

