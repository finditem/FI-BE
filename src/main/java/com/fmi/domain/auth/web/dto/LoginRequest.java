package com.fmi.domain.auth.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {
    @Schema(description = "이메일", example = "user@example.com")
    @Email @NotBlank private String email;

    @Schema(description = "비밀번호", example = "Abcd1234!")
    @NotBlank private String password;
}
