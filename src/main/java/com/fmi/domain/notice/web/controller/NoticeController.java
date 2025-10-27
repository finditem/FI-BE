package com.fmi.domain.notice.web.controller;

import com.fmi.domain.notice.data.enums.NoticeCategory;
import com.fmi.domain.notice.service.NoticeService;
import com.fmi.domain.notice.web.dto.NoticeListDTO;
import com.fmi.domain.notice.web.dto.NoticeResponseDTO;
import com.fmi.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notice")
@RequiredArgsConstructor
@Tag(name = "Notice", description = "공지사항 API")
public class NoticeController {
    
    private final NoticeService noticeService;
    
    /**
     * 공지사항 목록 조회
     * GET /api/notice?category=GENERAL&page=0&size=10
     */
    @GetMapping
    @Operation(summary = "공지사항 목록 조회", description = "상단 고정 공지사항이 먼저 표시됩니다.")
    public ApiResponse<Page<NoticeListDTO>> getNoticeList(
            @RequestParam(required = false) NoticeCategory category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        Pageable pageable = PageRequest.of(page, size, 
                Sort.by(Sort.Direction.DESC, "pinned")
                    .and(Sort.by(Sort.Direction.DESC, "createdAt")));
        
        Page<NoticeListDTO> notices;
        if (category != null) {
            notices = noticeService.getNoticeListByCategory(category, pageable);
        } else {
            notices = noticeService.getNoticeList(pageable);
        }
        
        return ApiResponse.onSuccess(notices);
    }
    
    /**
     * 공지사항 상세 조회
     * GET /api/notice/{noticeId}
     */
    @GetMapping("/{noticeId}")
    @Operation(summary = "공지사항 상세 조회", description = "조회수가 자동으로 증가합니다.")
    public ApiResponse<NoticeResponseDTO> getNoticeDetail(@PathVariable Long noticeId) {
        NoticeResponseDTO notice = noticeService.getNoticeDetail(noticeId);
        return ApiResponse.onSuccess(notice);
    }
}

