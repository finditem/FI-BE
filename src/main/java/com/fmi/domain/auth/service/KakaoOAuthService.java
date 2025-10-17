package com.fmi.domain.auth.service;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
@RequiredArgsConstructor
public class KakaoOAuthService {

    @Value("${KAKAO_REST_API_KEY:}")
    private String restApiKey;
    @Value("${KAKAO_CLIENT_SECRET:}")
    private String clientSecret; // 선택 항목
    @Value("${KAKAO_REDIRECT_URI:}")
    private String defaultRedirectUri; // 프론트에서 전달 가능, 없으면 기본 사용

    private static final String TOKEN_URL = "https://kauth.kakao.com/oauth/token";
    private static final String USERINFO_URL = "https://kapi.kakao.com/v2/user/me";

    public KakaoToken exchangeCodeForToken(String code, String redirectUri) {
        String useRedirect = (redirectUri != null && !redirectUri.isBlank()) ? redirectUri : defaultRedirectUri;

        RestTemplate rt = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "authorization_code");
        params.add("client_id", restApiKey);
        params.add("redirect_uri", useRedirect);
        params.add("code", code);
        if (clientSecret != null && !clientSecret.isBlank()) {
            params.add("client_secret", clientSecret);
        }

        HttpEntity<MultiValueMap<String, String>> req = new HttpEntity<>(params, headers);
        KakaoToken token = rt.postForObject(TOKEN_URL, req, KakaoToken.class);
        if (token == null || token.getAccess_token() == null) {
            throw new IllegalStateException("Kakao token exchange failed");
        }
        return token;
    }

    public KakaoUser getUserInfo(String accessToken) {
        RestTemplate rt = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + accessToken);
        HttpEntity<Void> req = new HttpEntity<>(headers);
        KakaoUser user = rt.postForObject(USERINFO_URL, req, KakaoUser.class);
        if (user == null || user.getId() == null) {
            throw new IllegalStateException("Kakao userinfo fetch failed");
        }
        return user;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class KakaoToken {
        private String token_type;
        private String access_token;
        private Integer expires_in;
        private String refresh_token;
        private Integer refresh_token_expires_in;
        private String scope;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class KakaoUser {
        private Long id;
        private KakaoAccount kakao_account;
        private Properties properties;

        @Data @NoArgsConstructor @AllArgsConstructor
        public static class KakaoAccount {
            private String email; // 선택 동의
            private Profile profile;

            @Data @NoArgsConstructor @AllArgsConstructor
            public static class Profile {
                private String nickname;
                private String profile_image_url;
            }
        }

        @Data @NoArgsConstructor @AllArgsConstructor
        public static class Properties {
            private String nickname;
            private String profile_image;
        }
    }
}


