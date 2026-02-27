package com.fmi.domain.notice.web.controller;

import com.fmi.domain.notice.data.enums.NoticeCategory;
import com.fmi.domain.notice.data.enums.NoticeSortType;
import com.fmi.domain.notice.service.NoticeService;
import com.fmi.domain.notice.web.dto.NoticeListDTO;
import com.fmi.domain.notice.web.dto.NoticeResponseDTO;
import com.fmi.global.apiPayload.ApiResponse;
import com.fmi.global.apiPayload.code.status.ErrorStatus;
import com.fmi.global.apiPayload.exception.GeneralException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/notices")
@RequiredArgsConstructor
@Tag(name = "Notice", description = "공지사항 API")
public class NoticeController {
    
    private final NoticeService noticeService;
    
    /**
     * 공지사항 목록 조회
     * GET /notices?category=GENERAL&page=0&size=10
     */
    @GetMapping
    @Operation(summary = "공지사항 목록 조회", description = "상단 고정 공지사항이 먼저 표시됩니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "공지사항 목록 조회 성공")
    })
    public ApiResponse<Page<NoticeListDTO>> getNoticeList(
            @RequestParam(required = false) NoticeCategory category,
            @RequestParam(required = false) String keyword,
            @Parameter(description = "정렬 기준 (LATEST=최신순, OLDEST=오래된순, MOST_VIEWED=조회많은순)")
            @RequestParam(required = false, defaultValue = "LATEST") NoticeSortType sortType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size, sortType.getSort());
        Page<NoticeListDTO> notices = noticeService.getNoticeList(category, keyword, sortType, pageable);

        return ApiResponse.onSuccess(notices);
    }
    
    /**
     * 공지사항 상세 조회
     * GET /notices/{noticeId}
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
            )
    })
    public ApiResponse<NoticeResponseDTO> getNoticeDetail(
            @PathVariable Long noticeId,
            HttpServletRequest request) {
        
        // 사용자 식별자 추출
        String userIdentifier;
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication != null && authentication.isAuthenticated() 
                && authentication.getPrincipal() instanceof UserDetails) {
            // 인증된 사용자: 이메일 사용
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            userIdentifier = userDetails.getUsername(); // 이메일
        } else {
            // 비로그인 사용자: IP 주소 사용 (각 사용자 개별 식별)
            userIdentifier = getClientIpAddress(request);
        }
        
        NoticeResponseDTO notice = noticeService.getNoticeDetail(noticeId, userIdentifier);
        return ApiResponse.onSuccess(notice);
    }
    
    /**
     * 공지사항 추천(좋아요) 추가
     * POST /notices/{noticeId}/like
     */
    @PostMapping("/{noticeId}/like")
    @Operation(summary = "공지사항 추천 추가", description = "공지사항을 추천합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "추천 성공")
    })
    public ApiResponse<Void> addLike(
            @PathVariable Long noticeId,
            @org.springframework.security.core.annotation.AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            throw new GeneralException(ErrorStatus._UNAUTHORIZED);
        }
        noticeService.addLike(noticeId, userDetails.getUsername());
        return ApiResponse.onSuccess(null);
    }

    /**
     * 공지사항 추천(좋아요) 취소
     * DELETE /notices/{noticeId}/like
     */
    @DeleteMapping("/{noticeId}/like")
    @Operation(summary = "공지사항 추천 취소", description = "공지사항 추천을 취소합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "추천 취소 성공")
    })
    public ApiResponse<Void> removeLike(
            @PathVariable Long noticeId,
            @org.springframework.security.core.annotation.AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            throw new GeneralException(ErrorStatus._UNAUTHORIZED);
        }
        noticeService.removeLike(noticeId, userDetails.getUsername());
        return ApiResponse.onSuccess(null);
    }

    /**
     * 클라이언트 IP 주소 추출 (비로그인 사용자 개별 식별용)
     */
    private String getClientIpAddress(HttpServletRequest request) {
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
        // X-Forwarded-For는 여러 IP가 있을 수 있으므로 첫 번째 IP만 사용
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip != null ? ip : "unknown";
    }
}

