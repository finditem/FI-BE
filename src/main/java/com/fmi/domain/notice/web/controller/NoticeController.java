package com.fmi.domain.notice.web.controller;

import com.fmi.domain.notice.data.enums.NoticeCategory;
import com.fmi.domain.notice.service.NoticeService;
import com.fmi.domain.notice.web.dto.NoticeListDTO;
import com.fmi.domain.notice.web.dto.NoticeResponseDTO;
import com.fmi.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/notice")
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
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "공지사항 목록 조회 성공"),
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
    @Operation(summary = "공지사항 상세 조회", description = "조회수가 5분마다만 증가합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "공지사항 상세 조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "NOTICE404-NOT_FOUND: 존재하지 않는 공지사항입니다",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = "{\"isSuccess\": false, \"code\": \"NOTICE404-NOT_FOUND\", \"message\": \"존재하지 않는 공지사항입니다.\"}"
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
    public ApiResponse<NoticeResponseDTO> getNoticeDetail(@PathVariable Long noticeId) {
        // 인증된 사용자의 이메일을 사용자 식별자로 사용
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userIdentifier = null;
        
        if (authentication != null && authentication.isAuthenticated() 
                && authentication.getPrincipal() instanceof UserDetails) {
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            userIdentifier = userDetails.getUsername(); // 이메일
        }
        
        // 인증되지 않은 경우 기본값 사용 (공개 API이므로)
        if (userIdentifier == null) {
            userIdentifier = "anonymous";
        }
        
        NoticeResponseDTO notice = noticeService.getNoticeDetail(noticeId, userIdentifier);
        return ApiResponse.onSuccess(notice);
    }
}

