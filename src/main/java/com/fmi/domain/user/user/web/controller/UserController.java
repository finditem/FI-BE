package com.fmi.domain.user.user.web.controller;

import com.fmi.domain.user.user.data.enums.UserOtherPageType;
import com.fmi.domain.user.user.service.UserProfileQueryService;
import com.fmi.domain.user.user.service.UserService;
import com.fmi.domain.user.user.web.dto.request.UserUpdateRequest;
import com.fmi.domain.user.user.web.dto.response.UserMetaResponse;
import com.fmi.domain.user.user.web.dto.response.UserOtherPageResponse;
import com.fmi.domain.user.user.web.dto.response.UserProfileResponse;
import com.fmi.domain.user.user.web.swagger.UserSwagger;
import com.fmi.global.apiPayload.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController implements UserSwagger {
    private final UserProfileQueryService userProfileQueryService;

    private final UserService userService;

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
            @Valid @RequestBody com.fmi.domain.user.user.web.dto.request.TermsAgreeRequest request) {
        String email = userDetails.getUsername();
        userService.agreeTerms(email, request);
        return ApiResponse.onSuccess(null);
    }

    @GetMapping("/{userId}/meta")
    @Override
    public ApiResponse<UserMetaResponse> getUserMeta(@PathVariable Long userId) {
        UserMetaResponse meta = userProfileQueryService.getUserMeta(userId);
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
        UserOtherPageResponse response =
                userProfileQueryService.getOtherUserPage(userId, type, userDetails, cursor, size);
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
