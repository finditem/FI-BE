package com.fmi.domain.auth.service.apple;

import org.jose4j.jws.JsonWebSignature;
import org.jose4j.jwt.JwtClaims;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;
import java.util.List;

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
}
