package com.fmi.domain.user.web.controller;

import com.fmi.domain.Enum.ActivityType;
import com.fmi.domain.Enum.Category;
import com.fmi.domain.Enum.SortType;
import com.fmi.domain.Enum.UserOtherPageType;
import com.fmi.domain.post.data.PostStatus;
import com.fmi.domain.post.data.PostType;
import com.fmi.domain.post.web.dto.response.PostPageResponse;
import com.fmi.domain.user.response.ImageUploadResponse;
import com.fmi.domain.user.response.MyActivityPageResponse;
import com.fmi.domain.user.response.MyCommentPageResponse;
import com.fmi.domain.user.response.PreferredLanguageResponse;
import com.fmi.domain.user.response.UserMetaResponse;
import com.fmi.domain.user.response.UserOtherPageResponse;
import com.fmi.domain.user.response.UserProfileResponse;
import com.fmi.domain.user.service.UserService;
import com.fmi.domain.user.web.dto.PreferredLanguageUpdateRequest;
import com.fmi.domain.user.web.dto.UserUpdateRequest;
import com.fmi.domain.user.web.swagger.UserSwagger;
import com.fmi.global.apiPayload.ApiResponse;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController implements UserSwagger {

    private final UserService userService;

    @PostMapping("/uploads/images")
    @Override
    public ApiResponse<ImageUploadResponse> uploadImages(@RequestPart(value = "images") List<MultipartFile> images) {
        ImageUploadResponse response = userService.uploadImages(images);
        return ApiResponse.onSuccess(response);
    }

    @GetMapping("/me")
    @Override
    public ApiResponse<UserProfileResponse> getMyProfile(@AuthenticationPrincipal UserDetails userDetails) {
        String email = userDetails.getUsername();
        UserProfileResponse response = userService.getMyProfile(email);
        return ApiResponse.onSuccess(response);
    }

    @PatchMapping("/me/terms")
    @Override
    public ApiResponse<Void> agreeTerms(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody com.fmi.domain.user.web.dto.TermsAgreeRequest request) {
        String email = userDetails.getUsername();
        userService.agreeTerms(email, request);
        return ApiResponse.onSuccess(null);
    }

    @GetMapping("/me/preferred-language")
    @Override
    public ApiResponse<PreferredLanguageResponse> getPreferredLanguage(
            @AuthenticationPrincipal UserDetails userDetails) {
        String email = userDetails.getUsername();
        PreferredLanguageResponse response = userService.getPreferredLanguage(email);
        return ApiResponse.onSuccess(response);
    }

    @PatchMapping("/me/preferred-language")
    @Override
    public ApiResponse<PreferredLanguageResponse> updatePreferredLanguage(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody PreferredLanguageUpdateRequest request) {
        String email = userDetails.getUsername();
        PreferredLanguageResponse response = userService.updatePreferredLanguage(email, request);
        return ApiResponse.onSuccess(response);
    }

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
                userService.getMyComments(email, sort, startDate, endDate, keyword, cursor, size);
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
        PostPageResponse response = userService.getMyPosts(
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
                userService.getMyActivities(email, type, startDate, endDate, keyword, cursor, size);
        return ApiResponse.onSuccess(response);
    }

    @GetMapping("/{userId}/meta")
    @Override
    public ApiResponse<UserMetaResponse> getUserMeta(@PathVariable Long userId) {
        UserMetaResponse meta = userService.getUserMeta(userId);
        return ApiResponse.onSuccess(meta);
    }

    @GetMapping("/{userId}/page")
    @Override
    public ApiResponse<UserOtherPageResponse> getUserOtherPage(
            @PathVariable Long userId,
            @RequestParam(required = false, defaultValue = "posts") UserOtherPageType type,
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "10") int size) {
        UserOtherPageResponse response = userService.getOtherUserPage(userId, type, userDetails, cursor, size);
        return ApiResponse.onSuccess(response);
    }

    @PatchMapping(value = "/me", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Override
    public ApiResponse<UserProfileResponse> updateMyProfile(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestPart(value = "request", required = false) @Valid UserUpdateRequest request,
            @RequestPart(value = "profileImage", required = false) MultipartFile profileImage) {
        String email = userDetails.getUsername();
        boolean deleteProfileImage = request != null && request.isDeleteProfileImage();
        UserProfileResponse response = userService.updateMyProfile(email, request, profileImage, deleteProfileImage);
        return ApiResponse.onSuccess(response);
    }
}
