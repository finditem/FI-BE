package com.fmi.config;

import com.fmi.global.apiPayload.code.status.ErrorStatus;
import com.fmi.global.apiPayload.exception.GeneralException;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;

@ConfigurationProperties(prefix = "oauth.apple")
public record AppleOAuthProperties(
        String teamId,
        String clientId,
        String keyId,
        String privateKeyBase64,
        Map<String, String> redirectUris
) {

    public AppleOAuthProperties {
        redirectUris = redirectUris == null ? Map.of() : Map.copyOf(redirectUris);
    }

    public String redirectUriFor(String environment) {
        String redirectUri = redirectUris.get(environment);
        if (redirectUri == null || redirectUri.isBlank()) {
            throw new GeneralException(ErrorStatus._APPLE_CODE_INVALID);
        }
        return redirectUri;
    }
}
