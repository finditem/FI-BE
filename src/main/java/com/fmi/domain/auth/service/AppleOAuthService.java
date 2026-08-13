package com.fmi.domain.auth.service;

import com.fmi.domain.auth.service.apple.AppleClientSecretGenerator;
import com.fmi.domain.auth.service.apple.AppleIdTokenVerifier;
import com.fmi.global.apiPayload.code.status.ErrorStatus;
import com.fmi.global.apiPayload.exception.GeneralException;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.jose4j.jwk.JsonWebKeySet;
import org.jose4j.keys.resolvers.JwksVerificationKeyResolver;
import org.jose4j.lang.JoseException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Map;

@Slf4j
@Service
public class AppleOAuthService {

    private static final String APPLE_TOKEN_URL = "https://appleid.apple.com/auth/token";
    private static final String APPLE_JWKS_URL = "https://appleid.apple.com/auth/keys";

    private final RestClient restClient;

    @Value("${APPLE_TEAM_ID:}")
    private String teamId;
    @Value("${APPLE_CLIENT_ID:}")
    private String clientId;
    @Value("${APPLE_CLIENT_ID_DEV:}")
    private String clientIdDev;
    @Value("${APPLE_CLIENT_ID_RELEASE:}")
    private String clientIdRelease;
    @Value("${APPLE_KEY_ID:}")
    private String keyId;
    @Value("${APPLE_PRIVATE_KEY_BASE64:}")
    private String privateKeyBase64;
    @Value("${APPLE_REDIRECT_URI:}")
    private String redirectUri;
    @Value("${APPLE_REDIRECT_URI_DEV:}")
    private String redirectUriDev;
    @Value("${APPLE_REDIRECT_URI_RELEASE:}")
    private String redirectUriRelease;

    public AppleOAuthService(RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder.build();
    }

    public String exchangeCodeForSubject(String code, String environment) {
        AppleOAuthConfig config = selectOAuthConfig(environment);
        String idToken = exchangeCodeForIdToken(code, config);
        return verifyAndGetSubject(idToken, config.clientId());
    }

    private String exchangeCodeForIdToken(String code, AppleOAuthConfig config) {
        MultiValueMap<String, String> form = authorizationCodeTokenRequest(code, config);

        try {
            AppleToken token = restClient.post()
                    .uri(APPLE_TOKEN_URL)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(AppleToken.class);
            if (token == null || token.getIdToken() == null || token.getIdToken().isBlank()) {
                throw new GeneralException(ErrorStatus._APPLE_TOKEN_FAILED);
            }
            return token.getIdToken();
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.BAD_REQUEST) {
                throw new GeneralException(ErrorStatus._APPLE_CODE_INVALID);
            }
            throw new GeneralException(ErrorStatus._APPLE_TOKEN_FAILED);
        } catch (RestClientException | IllegalStateException exception) {
            log.error("Apple token 교환 실패", exception);
            throw new GeneralException(ErrorStatus._APPLE_TOKEN_FAILED);
        }
    }

    private MultiValueMap<String, String> authorizationCodeTokenRequest(String code, AppleOAuthConfig config) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.setAll(Map.of(
                "grant_type", "authorization_code",
                "client_id", config.clientId(),
                "client_secret", new AppleClientSecretGenerator(teamId, config.clientId(), keyId, privateKeyBase64).generate(),
                "code", code,
                "redirect_uri", config.redirectUri()
        ));
        return form;
    }

    private String verifyAndGetSubject(String idToken, String selectedClientId) {
        try {
            String jwks = restClient.get()
                    .uri(APPLE_JWKS_URL)
                    .retrieve()
                    .body(String.class);
            if (jwks == null || jwks.isBlank()) {
                throw new GeneralException(ErrorStatus._APPLE_TOKEN_FAILED);
            }
            JsonWebKeySet jsonWebKeySet = new JsonWebKeySet(jwks);
            AppleIdTokenVerifier verifier = new AppleIdTokenVerifier(
                    selectedClientId,
                    new JwksVerificationKeyResolver(jsonWebKeySet.getJsonWebKeys())
            );
            return verifier.verifyAndGetSubject(idToken);
        } catch (GeneralException exception) {
            throw exception;
        } catch (RestClientException | JoseException | IllegalStateException exception) {
            log.error("Apple ID token 검증 준비 실패", exception);
            throw new GeneralException(ErrorStatus._APPLE_TOKEN_FAILED);
        }
    }

    private AppleOAuthConfig selectOAuthConfig(String environment) {
        boolean isDev = "dev".equalsIgnoreCase(environment);
        boolean isRelease = "release".equalsIgnoreCase(environment);

        if (isRelease && !clientIdRelease.isBlank()) {
            return new AppleOAuthConfig(
                    clientIdRelease,
                    redirectUriRelease.isBlank() ? redirectUri : redirectUriRelease
            );
        }
        if (isDev && !clientIdDev.isBlank()) {
            return new AppleOAuthConfig(
                    clientIdDev,
                    redirectUriDev.isBlank() ? redirectUri : redirectUriDev
            );
        }
        return new AppleOAuthConfig(clientId, redirectUri);
    }

    private record AppleOAuthConfig(String clientId, String redirectUri) {}

    @Data
    public static class AppleToken {
        private String id_token;

        public String getIdToken() {
            return id_token;
        }
    }
}
