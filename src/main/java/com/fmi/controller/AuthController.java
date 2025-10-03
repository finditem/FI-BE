package com.fmi.controller;

import com.fmi.global.apiPayload.ApiResponse;
import com.fmi.security.JwtTokenProvider;
import com.fmi.service.AuthService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import com.fmi.domain.Enum.Role;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.media.Schema;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "회원가입/로그인 API")
public class AuthController {

    private final AuthService authService;
    private final JwtTokenProvider jwtTokenProvider;

    @PostMapping("/signup")
    @Operation(summary = "회원가입", description = "이메일/비밀번호/닉네임/이름 등을 입력해 회원을 생성합니다. 비밀번호는 8자 이상, 대/소문자·숫자·특수문자를 포함해야 합니다.")
    public ApiResponse<SignupResponse> signup(@Valid @RequestBody SignupRequest request) {
        Long id = authService.signup(
                request.getEmail(),
                request.getPassword(),
                request.getNickname(),
                request.getName(),
                request.getPhoneNumber(),
                request.getProfileImg(),
                request.getRole(),
                request.getTermsOfServiceAgreed(),
                request.getPrivacyPolicyAgreed(),
                request.getMarketingConsent(),
                request.getTrustScore(),
                request.getEmailVerified(),
                request.getPhoneVerified()
        );
        return ApiResponse.onSuccess(new SignupResponse(id));
    }

    @PostMapping("/login")
    @Operation(summary = "로그인", description = "이메일과 비밀번호로 로그인합니다. 인증 실패 시 AUTH401-INVALID_CREDENTIALS가 반환됩니다.")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        var user = authService.authenticate(request.getEmail(), request.getPassword());
        var claims = new java.util.HashMap<String, Object>();
        claims.put("userId", user.getUserId());
        claims.put("role", user.getRole().name());
        String accessToken = jwtTokenProvider.createAccessToken(user.getEmail(), claims);
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getEmail());
        return ApiResponse.onSuccess(new LoginResponse(user.getUserId(), accessToken, refreshToken));
    }

    @Data
    public static class SignupRequest {
        @Schema(description = "이메일", example = "user@example.com")
        @Email
        @NotBlank
        private String email;
        @Schema(description = "비밀번호(규칙 충족)", example = "Abcd1234!")
        @NotBlank
        private String password;
        @Schema(description = "닉네임", example = "johnny")
        @NotBlank
        private String nickname;
        @Schema(description = "이름", example = "John Doe")
        @NotBlank
        private String name;

        // 선택/부가 정보
        @Schema(description = "전화번호", example = "+82-10-1234-5678")
        private String phoneNumber;
        @Schema(description = "프로필 이미지 URL", example = "https://example.com/images/johndoe.png")
        private String profileImg;
        @Schema(description = "역할", example = "USER")
        private Role role; // 기본값 USER (null이면 서버에서 설정)

        // 동의 항목
        @Schema(description = "이용약관 동의", example = "true")
        private Boolean termsOfServiceAgreed;
        @Schema(description = "개인정보 처리방침 동의", example = "true")
        private Boolean privacyPolicyAgreed;
        @Schema(description = "마케팅 수신 동의", example = "false")
        private Boolean marketingConsent;

        // 검증/점수(옵션)
        @Schema(description = "신뢰 점수", example = "75")
        private Long trustScore;
        @Schema(description = "이메일 인증 여부", example = "true")
        private Boolean emailVerified;
        @Schema(description = "전화번호 인증 여부", example = "true")
        private Boolean phoneVerified;
    }

    @Data
    @AllArgsConstructor
    public static class SignupResponse {
        private Long id;
    }

    @Data
    public static class LoginRequest {
        @Schema(description = "이메일", example = "user@example.com")
        @Email
        @NotBlank
        private String email;
        @Schema(description = "비밀번호", example = "Abcd1234!")
        @NotBlank
        private String password;
    }

    @Data
    @AllArgsConstructor
    public static class LoginResponse {
        private Long userId;
        private String accessToken;
        private String refreshToken;
    }
}


