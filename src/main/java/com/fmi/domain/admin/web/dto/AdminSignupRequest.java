package com.fmi.domain.admin.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AdminSignupRequest {
    @Schema(description = "이메일", example = "admin@example.com")
    @Email @NotBlank private String email;

    @Schema(description = "비밀번호(규칙 충족)", example = "Admin1234!")
    @NotBlank private String password;

    @Schema(description = "닉네임", example = "admin_user")
    @NotBlank private String nickname;

    @Schema(description = "이메일 인증 여부", example = "true")
    private Boolean emailVerified;
}
