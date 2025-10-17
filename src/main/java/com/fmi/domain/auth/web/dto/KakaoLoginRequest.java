package com.fmi.domain.auth.web.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class KakaoLoginRequest {
    @NotBlank(message = "grantType 필수")
    private String grantType; // authorization_code | access_token
    private String code;
    private String redirectUri;
    private String accessToken;
}

