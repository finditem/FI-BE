package com.fmi.domain.user.web.controller;

import com.fmi.domain.user.response.ImageUploadResponse;
import com.fmi.domain.user.response.UserProfileResponse;
import com.fmi.domain.user.service.UserService;
import com.fmi.domain.user.web.dto.PasswordChangeRequest;
import com.fmi.domain.user.web.dto.ProfileImageUpdateRequest;
import com.fmi.domain.user.web.dto.UserUpdateRequest;
import com.fmi.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Tag(name = "User", description = "사용자 정보 관리 API")
public class UserController {

    private final UserService userService;

    @PostMapping("/uploads/images")
    @Operation(summary = "이미지 업로드", description = "여러 장의 이미지를 S3에 업로드하고 URL을 반환합니다. (JPEG, PNG 형식만 지원)")
    public ApiResponse<ImageUploadResponse> uploadImages(
            @RequestPart(value = "images") List<MultipartFile> images
    ) {
        ImageUploadResponse response = userService.uploadImages(images);
        return ApiResponse.onSuccess(response);
    }

    @GetMapping("/me")
    @Operation(summary = "내 정보 조회", description = "현재 로그인한 사용자의 기본 프로필 정보를 조회합니다. (닉네임, 이메일, 이메일 인증 여부, 프로필 이미지)")
    public ApiResponse<UserProfileResponse> getMyProfile(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        String email = userDetails.getUsername();
        UserProfileResponse response = userService.getMyProfile(email);
        return ApiResponse.onSuccess(response);
    }

    @PatchMapping("/me")
    @Operation(summary = "내 정보 수정", description = "현재 로그인한 사용자의 닉네임을 수정합니다.")
    public ApiResponse<UserProfileResponse> updateMyProfile(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody UserUpdateRequest request
    ) {
        String email = userDetails.getUsername();
        UserProfileResponse response = userService.updateMyProfile(email, request);
        return ApiResponse.onSuccess(response);
    }

    @PatchMapping("/me/profile-image")
    @Operation(summary = "프로필 이미지 업데이트 및 삭제", 
               description = "프로필 이미지를 업데이트하거나 삭제합니다. profileImageUrl이 null이면 삭제, 값이 있으면 업데이트됩니다.")
    public ApiResponse<UserProfileResponse> updateProfileImage(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody ProfileImageUpdateRequest request
    ) {
        String email = userDetails.getUsername();
        UserProfileResponse response = userService.updateProfileImage(email, request);
        return ApiResponse.onSuccess(response);
    }

    @PatchMapping("/me/password")
    @Operation(summary = "비밀번호 변경", 
               description = "현재 비밀번호를 확인하고 새 비밀번호로 변경합니다. 새 비밀번호는 8~20자의 영문, 숫자, 특수문자를 포함해야 합니다.")
    public ApiResponse<Void> changePassword(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody PasswordChangeRequest request
    ) {
        String email = userDetails.getUsername();
        userService.changePassword(email, request);
        return ApiResponse.onSuccess(null);
    }

    @DeleteMapping("/me")
    @Operation(summary = "회원 탈퇴", 
               description = """
                   현재 로그인한 사용자의 계정을 소프트 삭제합니다.
                   
                   **삭제 방식:**
                   - 즉시 소프트 삭제: 계정은 삭제 표시되지만 30일간 데이터가 보관됩니다
                   - 프로필 이미지는 즉시 S3에서 삭제됩니다
                   - 30일 후 자동으로 완전 삭제(하드 삭제)됩니다
                   
                   **복구 및 재가입:**
                   - 30일 이내에는 복구가 가능합니다 (관리자 문의)
                   - 탈퇴 후 7일 이내에는 동일한 이메일로 재가입할 수 없습니다
                   - 7일 경과 후에는 동일한 이메일로 재가입이 가능합니다
                   
                   **주의사항:**
                   - 탈퇴 후에는 로그인 및 서비스 이용이 불가능합니다
                   - 작성한 게시글과 댓글은 자동으로 삭제되지 않으며, 익명화 처리될 수 있습니다
                   """)
    public ApiResponse<Void> deleteAccount(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        String email = userDetails.getUsername();
        userService.deleteAccount(email);
        return ApiResponse.onSuccess(null);
    }
}

