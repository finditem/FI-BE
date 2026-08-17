package com.fmi.domain.auth.service;

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
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
        appleOAuthService = new AppleOAuthService(restClientBuilder);

        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("EC");
        keyPairGenerator.initialize(256);
        KeyPair keyPair = keyPairGenerator.generateKeyPair();
        ReflectionTestUtils.setField(appleOAuthService, "teamId", "TEAM_ID");
        ReflectionTestUtils.setField(appleOAuthService, "clientId", "com.finditem.web");
        ReflectionTestUtils.setField(appleOAuthService, "clientIdDev", "com.finditem.dev");
        ReflectionTestUtils.setField(appleOAuthService, "clientIdRelease", "com.finditem.release");
        ReflectionTestUtils.setField(appleOAuthService, "keyId", "KEY_ID");
        ReflectionTestUtils.setField(
                appleOAuthService,
                "privateKeyBase64",
                Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded())
        );
        ReflectionTestUtils.setField(appleOAuthService, "redirectUri", "https://www.finditem.kr/auth/apple/callback");
        ReflectionTestUtils.setField(appleOAuthService, "redirectUriDev", "https://dev.finditem.kr/auth/apple/callback");
        ReflectionTestUtils.setField(appleOAuthService, "redirectUriRelease", "https://release.finditem.kr/auth/apple/callback");
    }

    @Test
    @SuppressWarnings("unchecked")
    void 유효하지_않은_Apple_인증_코드는_400_오류로_변환한다() {
        // given
        Apple_token_교환이_400으로_실패한다();

        // when
        Throwable exception = catchThrowable(() -> appleOAuthService.exchangeCodeForSubject("invalid-code", "dev"));

        // then
        ArgumentCaptor<MultiValueMap<String, String>> formCaptor = ArgumentCaptor.forClass(MultiValueMap.class);
        verify(requestBodyUriSpec).body(formCaptor.capture());
        assertThat(exception)
                .isInstanceOf(GeneralException.class)
                .extracting(actualException -> ((GeneralException) actualException).getCode())
                .isEqualTo(ErrorStatus._APPLE_CODE_INVALID);
        assertThat(formCaptor.getValue())
                .containsEntry("client_id", java.util.List.of("com.finditem.dev"))
                .containsEntry("redirect_uri", java.util.List.of("https://dev.finditem.kr/auth/apple/callback"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void dev_Client_ID가_없으면_prod_설정으로_대체한다() {
        // given
        ReflectionTestUtils.setField(appleOAuthService, "clientIdDev", "");
        Apple_token_교환이_400으로_실패한다();

        // when
        catchThrowable(() -> appleOAuthService.exchangeCodeForSubject("invalid-code", "dev"));

        // then
        ArgumentCaptor<MultiValueMap<String, String>> formCaptor = ArgumentCaptor.forClass(MultiValueMap.class);
        verify(requestBodyUriSpec).body(formCaptor.capture());
        assertThat(formCaptor.getValue())
                .containsEntry("client_id", java.util.List.of("com.finditem.web"))
                .containsEntry("redirect_uri", java.util.List.of("https://www.finditem.kr/auth/apple/callback"));
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
