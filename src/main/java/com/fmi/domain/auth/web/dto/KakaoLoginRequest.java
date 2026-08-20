package com.fmi.domain.auth.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
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
            required = true)
    @NotBlank(message = "code 필수") private String code;

    @Schema(
            description = "환경 타입 (선택사항: 'dev', 'release', 'prod'. 없으면 기본값 'prod' 사용)",
            example = "dev",
            allowableValues = {"dev", "release", "prod"})
    @Pattern(regexp = "^(dev|release|prod)$", message = "environment는 dev, release, prod 중 하나여야 합니다")
    private String environment;
}
