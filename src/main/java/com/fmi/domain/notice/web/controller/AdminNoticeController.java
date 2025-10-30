package com.fmi.domain.notice.web.controller;

import com.fmi.domain.notice.service.NoticeService;
import com.fmi.domain.notice.web.dto.NoticeCreateRequestDTO;
import com.fmi.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/notice")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin", description = "관리자 API")
public class AdminNoticeController {

    private final NoticeService noticeService;

    @PostMapping
    @Operation(summary = "공지 생성(관리자)")
    public ApiResponse<Long> createNotice(@RequestBody NoticeCreateRequestDTO request) {
        Long id = noticeService.createNotice(request);
        return ApiResponse.onSuccess(id);
    }
}


