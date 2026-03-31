package com.fmi.domain.auth.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class KakaoLoginRequest {
    @Schema(
        description = "카카오 인증 코드 (카카오 OAuth 인증 페이지에서 받은 authorization code)",
        example = "T84wymjsv8HheAUCfszclq-dceh33VhtqiVNftd5s2jqFVpC9t01dQAAAAQKDQ1fAAABm6cCC_3HP8VuE1ZNOQ",
        required = true
    )
    @NotBlank(message = "code 필수")
    private String code;

    @Schema(
        description = "환경 타입 (선택사항: 'dev' 또는 'prod', 없으면 기본값 'prod' 사용)",
        example = "dev",
        allowableValues = {"dev", "prod"}
    )
    private String environment;

    @Schema(description = "개인정보 처리방침 동의 (신규 가입 시 필수)", example = "true")
    private Boolean privacyPolicyAgreed;

    @Schema(description = "이용약관 동의 (신규 가입 시 필수)", example = "true")
    private Boolean termsOfServiceAgreed;

    @Schema(description = "콘텐츠 활용 동의 (선택)", example = "false")
    private Boolean contentPolicyAgreed;

    @Schema(description = "마케팅 수신 동의 (선택)", example = "false")
    private Boolean marketingConsent;
}

