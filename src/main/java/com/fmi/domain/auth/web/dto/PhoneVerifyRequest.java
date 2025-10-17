package com.fmi.domain.auth.web.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PhoneVerifyRequest {
    @NotBlank
    private String phone;
    @NotBlank
    private String code;
}

