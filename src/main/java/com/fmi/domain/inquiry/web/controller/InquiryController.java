package com.fmi.domain.inquiry.web.controller;

import com.fmi.domain.auth.data.User;
import com.fmi.domain.inquiry.data.enums.InquiryCategory;
import com.fmi.domain.inquiry.data.enums.InquiryStatus;
import com.fmi.domain.inquiry.service.InquiryService;
 
import com.fmi.domain.inquiry.web.dto.response.InquiryDetailDTO;
import com.fmi.domain.inquiry.web.dto.response.InquiryListDTO;
import com.fmi.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
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
@RequestMapping("/inquiries")
@RequiredArgsConstructor
@Tag(name = "Inquiry", description = "문의 API")
public class InquiryController {
    
    private final InquiryService inquiryService;
    
    /**
     * 공개 문의 작성
     * POST /api/inquiries
     */
    @PostMapping
    @Operation(summary = "문의 작성", description = "inquiryType=PUBLIC|PRIVATE 로 구분합니다. PUBLIC: 회원만 가능 + email 필수 / PRIVATE: 비회원 가능(필요 시 email 포함)")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "문의 작성 성공")
    })
    public ApiResponse<Long> createInquiry(
            @Valid @RequestBody com.fmi.domain.inquiry.web.dto.request.InquiryCreateRequestDTO request,
            @AuthenticationPrincipal User user) {
        Long inquiryId = inquiryService.createInquiry(request, user);
        return ApiResponse.onSuccess(inquiryId);
    }
    
    // 기존 /inquiries/public, /inquiries/private 엔드포인트는 제거되었습니다. 단일 POST /inquiries 를 사용하세요.
    
    /**
     * 공개 문의 목록 조회
     * GET /api/inquiries/public?category=GENERAL&status=PENDING&page=0&size=10
     */
    @GetMapping("/public")
    @Operation(summary = "공개 문의 목록 조회")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "공개 문의 목록 조회 성공")
    })
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
     * GET /api/inquiries/me?page=0&size=10
     */
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "내 문의 내역 조회", description = "공개/비공개 문의를 모두 조회합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "내 문의 내역 조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "COMMON401: 인증이 필요합니다",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = "{\"isSuccess\": false, \"code\": \"COMMON401\", \"message\": \"인증이 필요합니다.\"}"
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "COMMON500: 서버 에러",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = "{\"isSuccess\": false, \"code\": \"COMMON500\", \"message\": \"서버 에러, 관리자에게 문의 바랍니다.\"}"
                            )
                    )
            )
    })
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
     * GET /api/inquiries/{inquiryId}
     */
    @GetMapping("/{inquiryId}")
    @Operation(summary = "문의 상세 조회", description = "공개 문의는 누구나, 비공개 문의는 본인만 조회 가능합니다.")
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
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "COMMON500: 서버 에러",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = "{\"isSuccess\": false, \"code\": \"COMMON500\", \"message\": \"서버 에러, 관리자에게 문의 바랍니다.\"}"
                            )
                    )
            )
    })
    public ApiResponse<InquiryDetailDTO> getInquiryDetail(
            @PathVariable Long inquiryId,
            @AuthenticationPrincipal User user) {
        
        InquiryDetailDTO inquiry = inquiryService.getInquiryDetail(inquiryId, user);
        return ApiResponse.onSuccess(inquiry);
    }
}

