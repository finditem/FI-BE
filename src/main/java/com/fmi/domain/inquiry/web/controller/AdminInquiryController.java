package com.fmi.domain.inquiry.web.controller;

import com.fmi.domain.inquiry.service.InquiryService;
import com.fmi.domain.inquiry.web.dto.InquiryReplyCreateRequestDTO;
import com.fmi.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/inquiry")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin", description = "관리자 API")
public class AdminInquiryController {

    private final InquiryService inquiryService;

    @PostMapping("/{inquiryId}/reply")
    @Operation(summary = "문의 답변 등록(관리자)")
    public ApiResponse<Long> addReply(@PathVariable Long inquiryId,
                                      @RequestBody InquiryReplyCreateRequestDTO request) {
        Long replyId = inquiryService.addReply(inquiryId, request.getContent(), null);
        return ApiResponse.onSuccess(replyId);
    }
}


