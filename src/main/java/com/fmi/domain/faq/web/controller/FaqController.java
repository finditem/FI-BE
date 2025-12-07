package com.fmi.domain.faq.web.controller;

import com.fmi.domain.faq.data.enums.FaqCategory;
import com.fmi.domain.faq.service.FaqService;
import com.fmi.domain.faq.web.dto.FaqListDTO;
import com.fmi.domain.faq.web.dto.FaqResponseDTO;
import com.fmi.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/faq")
@RequiredArgsConstructor
@Tag(name = "FAQ", description = "자주 묻는 질문 API")
public class FaqController {
    
    private final FaqService faqService;
    
    /**
     * FAQ 목록 조회
     * GET /api/faq?category=USAGE&page=0&size=20
     */
    @GetMapping
    @Operation(summary = "FAQ 목록 조회", description = "카테고리별 필터링이 가능합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "FAQ 목록 조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "COMMON500: 서버 에러")
    })
    public ApiResponse<Page<FaqListDTO>> getFaqList(
            @RequestParam(required = false) FaqCategory category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "orderNum"));
        Page<FaqListDTO> faqs = faqService.getFaqList(category, pageable);
        return ApiResponse.onSuccess(faqs);
    }
    
    /**
     * FAQ 상세 조회
     * GET /api/faq/{faqId}
     */
    @GetMapping("/{faqId}")
    @Operation(summary = "FAQ 상세 조회", description = "조회수가 자동으로 증가합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "FAQ 상세 조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "FAQ404-NOT_FOUND: 존재하지 않는 FAQ입니다"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "COMMON500: 서버 에러")
    })
    public ApiResponse<FaqResponseDTO> getFaqDetail(@PathVariable Long faqId) {
        FaqResponseDTO faq = faqService.getFaqDetail(faqId);
        return ApiResponse.onSuccess(faq);
    }
}

