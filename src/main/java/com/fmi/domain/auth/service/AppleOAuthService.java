package com.fmi.domain.auth.service;

import com.fmi.config.AppleOAuthProperties;
import com.fmi.domain.auth.service.apple.AppleClientSecretGenerator;
import com.fmi.domain.auth.service.apple.AppleIdTokenVerifier;
import com.fmi.global.apiPayload.code.status.ErrorStatus;
import com.fmi.global.apiPayload.exception.GeneralException;
import java.util.Map;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.jose4j.jwk.JsonWebKeySet;
import org.jose4j.keys.resolvers.JwksVerificationKeyResolver;
import org.jose4j.lang.JoseException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Slf4j
@Service
public class AppleOAuthService {

    private static final String APPLE_TOKEN_URL = "https://appleid.apple.com/auth/token";
    private static final String APPLE_JWKS_URL = "https://appleid.apple.com/auth/keys";

    private final RestClient restClient;

    private final AppleOAuthProperties oauthProperties;

    public AppleOAuthService(RestClient.Builder restClientBuilder, AppleOAuthProperties oauthProperties) {
        this.restClient = restClientBuilder.build();
        this.oauthProperties = oauthProperties;
    }

    public String exchangeCodeForSubject(String code, String environment) {
        AppleOAuthConfig config = selectOAuthConfig(environment);
        String idToken = exchangeCodeForIdToken(code, config);
        return verifyAndGetSubject(idToken, config.clientId());
    }

    private String exchangeCodeForIdToken(String code, AppleOAuthConfig config) {
        MultiValueMap<String, String> form = authorizationCodeTokenRequest(code, config);

        try {
            AppleToken token = restClient
                    .post()
                    .uri(APPLE_TOKEN_URL)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(AppleToken.class);
            if (token == null
                    || token.getIdToken() == null
                    || token.getIdToken().isBlank()) {
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
                "client_secret",
                        new AppleClientSecretGenerator(
                                        oauthProperties.teamId(),
                                        config.clientId(),
                                        oauthProperties.keyId(),
                                        oauthProperties.privateKeyBase64())
                                .generate(),
                "code", code,
                "redirect_uri", config.redirectUri()));
        return form;
    }

    private String verifyAndGetSubject(String idToken, String selectedClientId) {
        try {
            String jwks = restClient.get().uri(APPLE_JWKS_URL).retrieve().body(String.class);
            if (jwks == null || jwks.isBlank()) {
                throw new GeneralException(ErrorStatus._APPLE_TOKEN_FAILED);
            }
            JsonWebKeySet jsonWebKeySet = new JsonWebKeySet(jwks);
            AppleIdTokenVerifier verifier = new AppleIdTokenVerifier(
                    selectedClientId, new JwksVerificationKeyResolver(jsonWebKeySet.getJsonWebKeys()));
            return verifier.verifyAndGetSubject(idToken);
        } catch (GeneralException exception) {
            throw exception;
        } catch (RestClientException | JoseException | IllegalStateException exception) {
            log.error("Apple ID token 검증 준비 실패", exception);
            throw new GeneralException(ErrorStatus._APPLE_TOKEN_FAILED);
        }
    }

    private AppleOAuthConfig selectOAuthConfig(String environment) {
        return new AppleOAuthConfig(oauthProperties.clientId(), oauthProperties.redirectUriFor(environment));
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
