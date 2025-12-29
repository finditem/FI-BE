package com.fmi.domain.auth.web.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class KakaoLoginRequest {
    @NotBlank(message = "code 필수")
    private String code;
    private String redirectUri;
}

