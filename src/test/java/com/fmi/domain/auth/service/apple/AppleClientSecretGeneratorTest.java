package com.fmi.domain.auth.service.apple;

import com.fmi.global.apiPayload.code.status.ErrorStatus;
import com.fmi.global.apiPayload.exception.GeneralException;
import org.jose4j.jws.JsonWebSignature;
import org.jose4j.jwt.JwtClaims;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AppleClientSecretGeneratorTest {

    @Test
    void Apple_client_secret은_ES256과_필수_claim을_가진다() throws Exception {
        // given
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("EC");
        keyPairGenerator.initialize(256);
        KeyPair keyPair = keyPairGenerator.generateKeyPair();
        String privateKeyBase64 = Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded());

        AppleClientSecretGenerator generator = new AppleClientSecretGenerator(
                "TEAM_ID",
                "com.finditem.web",
                "KEY_ID",
                privateKeyBase64
        );

        // when
        String clientSecret = generator.generate();

        // then
        JsonWebSignature jws = new JsonWebSignature();
        jws.setCompactSerialization(clientSecret);
        JwtClaims claims = JwtClaims.parse(jws.getUnverifiedPayload());
        String issuer = claims.getIssuer();
        String subject = claims.getSubject();
        List<String> audience = claims.getAudience();
        long validitySeconds = claims.getExpirationTime().getValue() - claims.getIssuedAt().getValue();

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(jws.getAlgorithmHeaderValue()).isEqualTo("ES256");
            softly.assertThat(jws.getKeyIdHeaderValue()).isEqualTo("KEY_ID");
            softly.assertThat(issuer).isEqualTo("TEAM_ID");
            softly.assertThat(subject).isEqualTo("com.finditem.web");
            softly.assertThat(audience).containsExactly("https://appleid.apple.com");
            softly.assertThat(validitySeconds).isBetween(1L, 15_777_000L);
        });
    }

    @Test
    void 유효하지_않은_개인키면_Apple_client_secret_생성_예외를_던진다() {
        // given
        AppleClientSecretGenerator generator = new AppleClientSecretGenerator(
                "TEAM_ID",
                "com.finditem.web",
                "KEY_ID",
                "invalid-private-key"
        );

        // when & then
        assertThatThrownBy(generator::generate)
                .isInstanceOf(GeneralException.class)
                .extracting(exception -> ((GeneralException) exception).getCode())
                .isEqualTo(ErrorStatus._APPLE_CLIENT_SECRET_GENERATION_FAILED);
    }

    @Test
    void Base64로_인코딩한_p8_PEM_개인키로_Apple_client_secret을_생성한다() throws Exception {
        // given
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("EC");
        keyPairGenerator.initialize(256);
        KeyPair keyPair = keyPairGenerator.generateKeyPair();
        String pem = "-----BEGIN PRIVATE KEY-----\n"
                + Base64.getMimeEncoder(64, "\n".getBytes(StandardCharsets.US_ASCII))
                .encodeToString(keyPair.getPrivate().getEncoded())
                + "\n-----END PRIVATE KEY-----\n";
        String privateKeyBase64 = Base64.getEncoder().encodeToString(pem.getBytes(StandardCharsets.US_ASCII));
        AppleClientSecretGenerator generator = new AppleClientSecretGenerator(
                "TEAM_ID",
                "com.finditem.web",
                "KEY_ID",
                privateKeyBase64
        );

        // when
        String clientSecret = generator.generate();

        // then
        JsonWebSignature jws = new JsonWebSignature();
        jws.setCompactSerialization(clientSecret);
        assertThat(jws.getAlgorithmHeaderValue()).isEqualTo("ES256");
    }
}
