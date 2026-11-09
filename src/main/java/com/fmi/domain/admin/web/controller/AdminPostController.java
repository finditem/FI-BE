package com.fmi.domain.admin.web.controller;

import com.fmi.domain.admin.service.AdminPostQueryService;
import com.fmi.domain.admin.web.swagger.AdminPostSwagger;
import com.fmi.domain.post.post.data.enums.Category;
import com.fmi.domain.post.post.data.enums.PostStatus;
import com.fmi.domain.post.post.web.dto.response.PostBriefResponse;
import com.fmi.global.apiPayload.ApiResponse;
import com.fmi.global.apiPayload.CursorPageResponse;
import com.fmi.global.enums.SortType;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminPostController implements AdminPostSwagger {

    private final AdminPostQueryService adminPostQueryService;

    @GetMapping("/posts/content-policy")
    @Override
    public ApiResponse<CursorPageResponse<PostBriefResponse>> getContentPolicyPosts(
            @RequestParam(defaultValue = "LATEST") SortType sortType,
            @RequestParam(required = false) Category category,
            @RequestParam(required = false) PostStatus postStatus,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false)
                    @org.springframework.format.annotation.DateTimeFormat(
                            iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE)
                    java.time.LocalDate startDate,
            @RequestParam(required = false)
                    @org.springframework.format.annotation.DateTimeFormat(
                            iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE)
                    java.time.LocalDate endDate,
            @RequestParam(required = false) Long cursor,
            @RequestParam(required = false) Long cursorViewCount,
            @RequestParam(required = false) Long cursorFavCount,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserDetails userDetails) {
        CursorPageResponse<PostBriefResponse> response = adminPostQueryService.getContentPolicyPosts(
                sortType,
                category,
                postStatus,
                keyword,
                startDate,
                endDate,
                cursor,
                cursorViewCount,
                cursorFavCount,
                size,
                userDetails.getUsername());
        return ApiResponse.onSuccess(response);
    }
}
