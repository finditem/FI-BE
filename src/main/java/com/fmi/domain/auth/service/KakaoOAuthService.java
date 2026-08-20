package com.fmi.domain.auth.service;

import com.fmi.config.KakaoOAuthProperties;
import com.fmi.global.apiPayload.code.status.ErrorStatus;
import com.fmi.global.apiPayload.exception.GeneralException;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
@RequiredArgsConstructor
public class KakaoOAuthService {

    private final KakaoOAuthProperties oauthProperties;

    private static final String TOKEN_URL = "https://kauth.kakao.com/oauth/token";
    private static final String USERINFO_URL = "https://kapi.kakao.com/v2/user/me";
    private static final String UNLINK_URL = "https://kapi.kakao.com/v1/user/unlink";

    public KakaoToken exchangeCodeForToken(String code) {
        return exchangeCodeForToken(code, null);
    }

    public KakaoToken exchangeCodeForToken(String code, String environment) {
        RestTemplate rt = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        String finalRestApiKey = oauthProperties.clientId();
        String finalRedirectUri = oauthProperties.redirectUriFor(environment);
        String finalClientSecret = oauthProperties.clientSecret();

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "authorization_code");
        params.add("client_id", finalRestApiKey);
        params.add("redirect_uri", finalRedirectUri);
        params.add("code", code);
        if (finalClientSecret != null && !finalClientSecret.isBlank()) {
            params.add("client_secret", finalClientSecret);
        }

        HttpEntity<MultiValueMap<String, String>> req = new HttpEntity<>(params, headers);

        try {
            ResponseEntity<KakaoToken> response = rt.postForEntity(TOKEN_URL, req, KakaoToken.class);
            KakaoToken token = response.getBody();

            if (token == null || token.getAccess_token() == null) {
                log.error("카카오 토큰 교환 실패: 응답이 null이거나 access_token이 없습니다.");
                throw new GeneralException(ErrorStatus._KAKAO_TOKEN_EXCHANGE_FAILED);
            }

            log.info("카카오 토큰 교환 성공");
            return token;
        } catch (HttpClientErrorException e) {
            log.error("카카오 토큰 교환 실패: HTTP {} - {}", e.getStatusCode(), e.getResponseBodyAsString());

            // 400 에러인 경우 (인증 코드 재사용, 만료, 잘못된 코드 등)
            if (e.getStatusCode() == HttpStatus.BAD_REQUEST) {
                String errorBody = e.getResponseBodyAsString();
                log.error("카카오 인증 코드 오류: {}", errorBody);
                throw new GeneralException(ErrorStatus._KAKAO_CODE_INVALID);
            }

            // 401 Unauthorized (잘못된 API 키 등)
            if (e.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                throw new GeneralException(ErrorStatus._KAKAO_TOKEN_EXCHANGE_FAILED);
            }

            throw new GeneralException(ErrorStatus._KAKAO_TOKEN_EXCHANGE_FAILED);
        } catch (RestClientException e) {
            log.error("카카오 토큰 교환 실패: 네트워크 오류", e);
            throw new GeneralException(ErrorStatus._KAKAO_TOKEN_EXCHANGE_FAILED);
        }
    }

    public KakaoUser getUserInfo(String accessToken) {
        RestTemplate rt = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + accessToken);
        HttpEntity<Void> req = new HttpEntity<>(headers);

        try {
            ResponseEntity<KakaoUser> response = rt.postForEntity(USERINFO_URL, req, KakaoUser.class);
            KakaoUser user = response.getBody();

            if (user == null || user.getId() == null) {
                log.error("카카오 사용자 정보 조회 실패: 응답이 null이거나 id가 없습니다.");
                throw new GeneralException(ErrorStatus._KAKAO_USERINFO_FETCH_FAILED);
            }

            log.info("카카오 사용자 정보 조회 성공: userId={}", user.getId());
            return user;
        } catch (HttpClientErrorException e) {
            log.error("카카오 사용자 정보 조회 실패: HTTP {} - {}", e.getStatusCode(), e.getResponseBodyAsString());

            // 401 Unauthorized (잘못된 토큰)
            if (e.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                log.error("카카오 access token이 유효하지 않습니다.");
                throw new GeneralException(ErrorStatus._KAKAO_USERINFO_FETCH_FAILED);
            }

            // 403 Forbidden (권한 없음)
            if (e.getStatusCode() == HttpStatus.FORBIDDEN) {
                log.error("카카오 사용자 정보 조회 권한이 없습니다.");
                throw new GeneralException(ErrorStatus._KAKAO_USERINFO_FETCH_FAILED);
            }

            throw new GeneralException(ErrorStatus._KAKAO_USERINFO_FETCH_FAILED);
        } catch (RestClientException e) {
            log.error("카카오 사용자 정보 조회 실패: 네트워크 오류", e);
            throw new GeneralException(ErrorStatus._KAKAO_USERINFO_FETCH_FAILED);
        }
    }

    public void unlinkUser(String kakaoUserId) {
        RestTemplate rt = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.set("Authorization", "KakaoAK " + oauthProperties.adminKey());

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("target_id_type", "user_id");
        params.add("target_id", kakaoUserId);

        HttpEntity<MultiValueMap<String, String>> req = new HttpEntity<>(params, headers);

        try {
            rt.postForEntity(UNLINK_URL, req, String.class);
            log.info("카카오 연결 끊기 성공: kakaoUserId={}", kakaoUserId);
        } catch (RestClientException e) {
            log.warn("카카오 연결 끊기 실패: kakaoUserId={}, error={}", kakaoUserId, e.getMessage());
        }
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

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        public static class KakaoAccount {
            private String email; // 선택 동의
            private Profile profile;

            @Data
            @NoArgsConstructor
            @AllArgsConstructor
            public static class Profile {
                private String nickname;
                private String profile_image_url;
            }
        }

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        public static class Properties {
            private String nickname;
            private String profile_image;
        }
    }
}
