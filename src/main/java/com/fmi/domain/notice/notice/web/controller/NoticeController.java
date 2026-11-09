package com.fmi.domain.notice.notice.web.controller;

import com.fmi.domain.notice.notice.data.enums.NoticeCategory;
import com.fmi.domain.notice.notice.data.enums.NoticeSortType;
import com.fmi.domain.notice.notice.service.NoticeDetailService;
import com.fmi.domain.notice.notice.service.NoticeQueryService;
import com.fmi.domain.notice.notice.web.dto.response.NoticeListResponse;
import com.fmi.domain.notice.notice.web.dto.response.NoticeMetaResponse;
import com.fmi.domain.notice.notice.web.dto.response.NoticeResponse;
import com.fmi.domain.notice.notice.web.swagger.NoticeSwagger;
import com.fmi.global.apiPayload.ApiResponse;
import com.fmi.global.apiPayload.CursorPageResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/notices")
@RequiredArgsConstructor
public class NoticeController implements NoticeSwagger {
    private final NoticeDetailService noticeDetailService;

    private final NoticeQueryService noticeQueryService;

    /**
     * 공지사항 목록 조회
     * GET /notices?category=GENERAL&cursor=10&size=10
     */
    @GetMapping
    @Override
    public ApiResponse<CursorPageResponse<NoticeListResponse>> getNoticeList(
            @RequestParam(required = false) NoticeCategory category,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false, defaultValue = "LATEST") NoticeSortType sortType,
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "10") int size) {

        CursorPageResponse<NoticeListResponse> notices =
                noticeQueryService.getNoticeListCursor(category, keyword, sortType, cursor, size);
        return ApiResponse.onSuccess(notices);
    }

    /**
     * 공지사항 메타데이터 조회 (OG 태그용)
     * GET /notices/{noticeId}/meta
     */
    @GetMapping("/{noticeId}/meta")
    @Override
    public ApiResponse<NoticeMetaResponse> getNoticeMeta(@PathVariable Long noticeId) {
        NoticeMetaResponse meta = noticeQueryService.getNoticeMeta(noticeId);
        return ApiResponse.onSuccess(meta);
    }

    /**
     * 공지사항 상세 조회
     * GET /notices/{noticeId}
     */
    @GetMapping("/{noticeId}")
    @Override
    public ApiResponse<NoticeResponse> getNoticeDetail(@PathVariable Long noticeId, HttpServletRequest request) {

        // 사용자 식별자 추출
        String userIdentifier;
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null
                && authentication.isAuthenticated()
                && authentication.getPrincipal() instanceof UserDetails) {
            // 인증된 사용자: 이메일 사용
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            userIdentifier = userDetails.getUsername(); // 이메일
        } else {
            // 비로그인 사용자: IP 주소 사용 (각 사용자 개별 식별)
            userIdentifier = getClientIpAddress(request);
        }

        UserDetails userDetails = (authentication != null && authentication.getPrincipal() instanceof UserDetails)
                ? (UserDetails) authentication.getPrincipal()
                : null;

        NoticeResponse notice = noticeDetailService.getNoticeDetail(noticeId, userIdentifier, userDetails);

        return ApiResponse.onSuccess(notice);
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
