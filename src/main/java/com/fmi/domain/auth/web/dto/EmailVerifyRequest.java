package com.fmi.domain.auth.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class EmailVerifyRequest {
    @Email @NotBlank
    private String email;
    @NotBlank
    private String code;
}

