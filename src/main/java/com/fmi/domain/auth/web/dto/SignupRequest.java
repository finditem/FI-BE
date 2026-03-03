package com.fmi.domain.auth.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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

    // 동의 항목 (모두 필수 - 값은 반드시 전송해야 함)
    @Schema(description = "개인정보 처리방침 동의", example = "true")
    @NotNull(message = "개인정보 처리방침 동의 값을 입력해주세요.")
    private Boolean privacyPolicyAgreed;
    
    @Schema(description = "마케팅 수신 동의", example = "false")
    @NotNull(message = "마케팅 수신 동의 값을 입력해주세요.")
    private Boolean marketingConsent;

    // 이메일 인증은 백엔드에서 Redis를 통해 관리하므로 프론트엔드에서 전송 불필요
}

