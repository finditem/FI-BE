package com.fmi.domain.auth.web.dto;

import com.fmi.domain.Enum.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SignupRequest {
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

