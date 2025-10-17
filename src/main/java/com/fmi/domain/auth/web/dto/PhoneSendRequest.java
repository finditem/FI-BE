package com.fmi.domain.auth.web.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PhoneSendRequest {
    @NotBlank
    private String phone;
}

