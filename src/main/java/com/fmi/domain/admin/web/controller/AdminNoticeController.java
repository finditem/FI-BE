package com.fmi.domain.admin.web.controller;

import com.fmi.domain.admin.web.swagger.AdminNoticeSwagger;
import com.fmi.domain.notice.notice.service.NoticeService;
import com.fmi.domain.notice.notice.web.dto.request.NoticeCreateRequest;
import com.fmi.domain.notice.notice.web.dto.request.NoticeUpdateRequest;
import com.fmi.domain.notice.notice.web.dto.response.NoticeResponse;
import com.fmi.global.apiPayload.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminNoticeController implements AdminNoticeSwagger {

    private final NoticeService noticeService;

    @PostMapping("/notices")
    @Override
    public ApiResponse<Long> createNotice(
            @Valid @RequestBody NoticeCreateRequest request, @AuthenticationPrincipal UserDetails userDetails) {
        Long id = noticeService.createNotice(request, userDetails.getUsername());
        return ApiResponse.onSuccess(id);
    }

    @GetMapping("/notices/draft")
    @Override
    public ApiResponse<NoticeResponse> getDraftNotice(@AuthenticationPrincipal UserDetails userDetails) {
        NoticeResponse response = noticeService.getDraftNotice(userDetails.getUsername());
        return ApiResponse.onSuccess(response);
    }

    @PutMapping("/notices/{noticeId}")
    @Override
    public ApiResponse<NoticeResponse> updateNotice(
            @PathVariable Long noticeId, @Valid @RequestBody NoticeUpdateRequest request) {
        NoticeResponse response = noticeService.updateNotice(noticeId, request);
        return ApiResponse.onSuccess(response);
    }

    @DeleteMapping("/notices/{noticeId}")
    @Override
    public ApiResponse<String> deleteNotice(@PathVariable Long noticeId) {
        noticeService.deleteNotice(noticeId);
        return ApiResponse.onSuccess("공지사항 삭제 완료");
    }
}
