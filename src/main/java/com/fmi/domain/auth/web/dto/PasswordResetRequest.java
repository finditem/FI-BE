package com.fmi.domain.auth.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import lombok.Data;

@Data
public class PasswordResetRequest {
    @Schema(description = "가입 이메일", example = "user@example.com")
    @Email
    private String email;
}

