package com.fmi.config;

import com.fmi.global.apiPayload.code.status.ErrorStatus;
import com.fmi.global.apiPayload.exception.GeneralException;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "oauth.kakao")
public record KakaoOAuthProperties(
        String clientId, String clientSecret, String adminKey, Map<String, String> redirectUris) {

    public KakaoOAuthProperties {
        redirectUris = redirectUris == null ? Map.of() : Map.copyOf(redirectUris);
    }

    public String redirectUriFor(String environment) {
        String redirectUri = redirectUris.get(environment);
        if (redirectUri == null || redirectUri.isBlank()) {
            throw new GeneralException(ErrorStatus._KAKAO_CODE_INVALID);
        }
        return redirectUri;
    }
}
