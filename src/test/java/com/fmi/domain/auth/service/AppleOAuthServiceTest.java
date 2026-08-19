package com.fmi.domain.auth.service;

import com.fmi.config.AppleOAuthProperties;
import com.fmi.global.apiPayload.code.status.ErrorStatus;
import com.fmi.global.apiPayload.exception.GeneralException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppleOAuthServiceTest {

    @Mock
    private RestClient.Builder restClientBuilder;
    @Mock
    private RestClient restClient;
    @Mock
    private RestClient.RequestBodyUriSpec requestBodyUriSpec;
    @Mock
    private RestClient.RequestBodySpec requestBodySpec;
    @Mock
    private RestClient.ResponseSpec responseSpec;

    private AppleOAuthService appleOAuthService;

    @BeforeEach
    void setUp() throws Exception {
        when(restClientBuilder.build()).thenReturn(restClient);

        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("EC");
        keyPairGenerator.initialize(256);
        KeyPair keyPair = keyPairGenerator.generateKeyPair();
        AppleOAuthProperties properties = new AppleOAuthProperties(
                "TEAM_ID",
                "com.finditem.test",
                "KEY_ID",
                Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded()),
                Map.of(
                        "dev", "https://dev.finditem.kr/auth/apple/callback",
                        "release", "https://release.finditem.kr/auth/apple/callback"
                )
        );
        appleOAuthService = new AppleOAuthService(restClientBuilder, properties);
    }

    @Test
    @SuppressWarnings("unchecked")
    void dev_채널은_테스트_Client_ID와_dev_redirect_URI를_사용한다() {
        Apple_token_교환이_400으로_실패한다();

        Throwable exception = catchThrowable(() -> appleOAuthService.exchangeCodeForSubject("invalid-code", "dev"));

        ArgumentCaptor<MultiValueMap<String, String>> formCaptor = ArgumentCaptor.forClass(MultiValueMap.class);
        verify(requestBodyUriSpec).body(formCaptor.capture());
        assertThat(exception)
                .isInstanceOf(GeneralException.class)
                .extracting(actualException -> ((GeneralException) actualException).getCode())
                .isEqualTo(ErrorStatus._APPLE_CODE_INVALID);
        assertThat(formCaptor.getValue())
                .containsEntry("client_id", java.util.List.of("com.finditem.test"))
                .containsEntry("redirect_uri", java.util.List.of("https://dev.finditem.kr/auth/apple/callback"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void release_채널은_같은_테스트_Client_ID와_release_redirect_URI를_사용한다() {
        Apple_token_교환이_400으로_실패한다();

        catchThrowable(() -> appleOAuthService.exchangeCodeForSubject("invalid-code", "release"));

        ArgumentCaptor<MultiValueMap<String, String>> formCaptor = ArgumentCaptor.forClass(MultiValueMap.class);
        verify(requestBodyUriSpec).body(formCaptor.capture());
        assertThat(formCaptor.getValue())
                .containsEntry("client_id", java.util.List.of("com.finditem.test"))
                .containsEntry("redirect_uri", java.util.List.of("https://release.finditem.kr/auth/apple/callback"));
    }

    @Test
    void 허용하지_않은_채널은_token_교환_전에_400_오류를_반환한다() {
        Throwable exception = catchThrowable(() -> appleOAuthService.exchangeCodeForSubject("invalid-code", "prod"));

        assertThat(exception)
                .isInstanceOf(GeneralException.class)
                .extracting(actualException -> ((GeneralException) actualException).getCode())
                .isEqualTo(ErrorStatus._APPLE_CODE_INVALID);
    }

    private void Apple_token_교환이_400으로_실패한다() {
        when(restClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri("https://appleid.apple.com/auth/token")).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.contentType(MediaType.APPLICATION_FORM_URLENCODED)).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.body(any(MultiValueMap.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(AppleOAuthService.AppleToken.class))
                .thenThrow(new HttpClientErrorException(HttpStatus.BAD_REQUEST));
    }
}
