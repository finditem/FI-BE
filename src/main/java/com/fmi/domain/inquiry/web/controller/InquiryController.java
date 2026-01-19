package com.fmi.domain.inquiry.web.controller;

import com.fmi.domain.auth.data.User;
import com.fmi.domain.auth.repository.UserRepository;
import com.fmi.domain.inquiry.service.InquiryService;
 
import com.fmi.domain.inquiry.web.dto.response.InquiryDetailDTO;
import com.fmi.domain.inquiry.web.dto.response.InquiryListDTO;
import com.fmi.global.apiPayload.ApiResponse;
import com.fmi.global.apiPayload.code.status.ErrorStatus;
import com.fmi.global.apiPayload.exception.GeneralException;
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
    @Operation(summary = "1:1 개인 문의 작성", description = "비회원도 가능하며, 비회원인 경우 email을 포함할 수 있습니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "문의 작성 성공")
    })
    public ApiResponse<Long> createInquiry(
            @Valid @RequestBody com.fmi.domain.inquiry.web.dto.request.InquiryCreateRequestDTO request,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = null;
        if (userDetails != null) {
            user = userRepository.findByEmail(userDetails.getUsername())
                    .orElseThrow(() -> new GeneralException(ErrorStatus._USER_NOT_FOUND));
        }
        Long inquiryId = inquiryService.createInquiry(request, user);
        return ApiResponse.onSuccess(inquiryId);
    }
    
    /**
     * 내 문의 내역 조회
     * GET /api/inquiries/me?page=0&size=10
     */
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "내 문의 내역 조회", description = "본인의 1:1 개인 문의를 조회합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "내 문의 내역 조회 성공")
    })
    public ApiResponse<Page<InquiryListDTO>> getMyInquiries(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new GeneralException(ErrorStatus._USER_NOT_FOUND));
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<InquiryListDTO> inquiries = inquiryService.getMyInquiries(user, pageable);
        return ApiResponse.onSuccess(inquiries);
    }
    
    /**
     * 문의 상세 조회
     * GET /api/inquiries/{inquiryId}
     */
    @GetMapping("/{inquiryId}")
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
        
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new GeneralException(ErrorStatus._USER_NOT_FOUND));
        InquiryDetailDTO inquiry = inquiryService.getInquiryDetail(inquiryId, user);
        return ApiResponse.onSuccess(inquiry);
    }
}

