package com.fmi.domain.user.user.web.controller;

import com.fmi.domain.post.post.data.enums.Category;
import com.fmi.domain.post.post.data.enums.PostStatus;
import com.fmi.domain.post.post.data.enums.PostType;
import com.fmi.domain.post.post.web.dto.response.PostPageResponse;
import com.fmi.domain.user.user.data.enums.ActivityType;
import com.fmi.domain.user.user.service.UserActivityQueryService;
import com.fmi.domain.user.user.web.dto.response.MyActivityPageResponse;
import com.fmi.domain.user.user.web.dto.response.MyCommentPageResponse;
import com.fmi.domain.user.user.web.swagger.UserActivitySwagger;
import com.fmi.global.apiPayload.ApiResponse;
import com.fmi.global.enums.SortType;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserActivityController implements UserActivitySwagger {
    private final UserActivityQueryService userActivityQueryService;

    @GetMapping("/me/comments")
    @Override
    public ApiResponse<MyCommentPageResponse> getMyComments(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false, defaultValue = "LATEST") SortType sort,
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "20") int size) {
        String email = userDetails.getUsername();
        MyCommentPageResponse response =
                userActivityQueryService.getMyComments(email, sort, startDate, endDate, keyword, cursor, size);
        return ApiResponse.onSuccess(response);
    }

    @GetMapping("/me/posts")
    @Override
    public ApiResponse<PostPageResponse> getMyPosts(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) PostType postType,
            @RequestParam(required = false) PostStatus postStatus,
            @RequestParam(required = false) Category category,
            @RequestParam(required = false, defaultValue = "LATEST") SortType sortType,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "20") int size) {
        String email = userDetails.getUsername();
        PostPageResponse response = userActivityQueryService.getMyPosts(
                email, postType, postStatus, category, sortType, startDate, endDate, keyword, cursor, size);
        return ApiResponse.onSuccess(response);
    }

    @GetMapping("/me/activities")
    @Override
    public ApiResponse<MyActivityPageResponse> getMyActivities(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) ActivityType type,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int size) {
        String email = userDetails.getUsername();
        MyActivityPageResponse response =
                userActivityQueryService.getMyActivities(email, type, startDate, endDate, keyword, cursor, size);
        return ApiResponse.onSuccess(response);
    }
}
