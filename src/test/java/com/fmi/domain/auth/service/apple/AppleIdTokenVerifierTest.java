package com.fmi.domain.auth.service.apple;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import com.fmi.global.apiPayload.code.status.ErrorStatus;
import com.fmi.global.apiPayload.exception.GeneralException;
import org.jose4j.jwk.JsonWebKeySet;
import org.jose4j.jwk.RsaJsonWebKey;
import org.jose4j.jwk.RsaJwkGenerator;
import org.jose4j.jws.AlgorithmIdentifiers;
import org.jose4j.jws.JsonWebSignature;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.keys.resolvers.JwksVerificationKeyResolver;
import org.junit.jupiter.api.Test;

class AppleIdTokenVerifierTest {

    @Test
    void 유효한_Apple_ID_token이면_subject를_반환한다() throws Exception {
        // given
        RsaJsonWebKey signingJwk = RsaJwkGenerator.generateJwk(2048);
        signingJwk.setKeyId("apple-key");
        AppleIdTokenVerifier verifier = 검증기(signingJwk);
        String idToken = 서명한_ID_token(signingJwk, "com.finditem.web", "apple-subject");

        // when
        String subject = verifier.verifyAndGetSubject(idToken);

        // then
        assertThat(subject).isEqualTo("apple-subject");
    }

    @Test
    void 예상과_다른_audience를_가진_Apple_ID_token은_거부한다() throws Exception {
        // given
        RsaJsonWebKey signingJwk = RsaJwkGenerator.generateJwk(2048);
        signingJwk.setKeyId("apple-key");
        AppleIdTokenVerifier verifier = 검증기(signingJwk);
        String idToken = 서명한_ID_token(signingJwk, "unexpected-client", "apple-subject");

        // when
        Throwable exception = catchThrowable(() -> verifier.verifyAndGetSubject(idToken));

        // then
        assertThat(exception)
                .isInstanceOf(GeneralException.class)
                .extracting(actualException -> ((GeneralException) actualException).getCode())
                .isEqualTo(ErrorStatus._APPLE_ID_TOKEN_INVALID);
    }

    @Test
    void 만료된_Apple_ID_token은_거부한다() throws Exception {
        // given
        RsaJsonWebKey signingJwk = RsaJwkGenerator.generateJwk(2048);
        signingJwk.setKeyId("apple-key");
        AppleIdTokenVerifier verifier = 검증기(signingJwk);
        String idToken = 만료된_서명_ID_token(signingJwk, "com.finditem.web", "apple-subject");

        // when
        Throwable exception = catchThrowable(() -> verifier.verifyAndGetSubject(idToken));

        // then
        assertThat(exception)
                .isInstanceOf(GeneralException.class)
                .extracting(actualException -> ((GeneralException) actualException).getCode())
                .isEqualTo(ErrorStatus._APPLE_ID_TOKEN_INVALID);
    }

    @Test
    void Apple_JWK에_없는_키로_서명한_ID_token은_거부한다() throws Exception {
        // given
        RsaJsonWebKey trustedJwk = RsaJwkGenerator.generateJwk(2048);
        trustedJwk.setKeyId("trusted-key");
        RsaJsonWebKey unknownJwk = RsaJwkGenerator.generateJwk(2048);
        unknownJwk.setKeyId("unknown-key");
        AppleIdTokenVerifier verifier = 검증기(trustedJwk);
        String idToken = 서명한_ID_token(unknownJwk, "com.finditem.web", "apple-subject");

        // when
        Throwable exception = catchThrowable(() -> verifier.verifyAndGetSubject(idToken));

        // then
        assertThat(exception)
                .isInstanceOf(GeneralException.class)
                .extracting(actualException -> ((GeneralException) actualException).getCode())
                .isEqualTo(ErrorStatus._APPLE_ID_TOKEN_INVALID);
    }

    private AppleIdTokenVerifier 검증기(RsaJsonWebKey signingJwk) {
        return new AppleIdTokenVerifier(
                "com.finditem.web", new JwksVerificationKeyResolver(new JsonWebKeySet(signingJwk).getJsonWebKeys()));
    }

    private String 서명한_ID_token(RsaJsonWebKey signingJwk, String audience, String subject) throws Exception {
        JwtClaims claims = new JwtClaims();
        claims.setIssuer("https://appleid.apple.com");
        claims.setAudience(audience);
        claims.setSubject(subject);
        claims.setIssuedAtToNow();
        claims.setExpirationTimeMinutesInTheFuture(5);

        JsonWebSignature jws = new JsonWebSignature();
        jws.setAlgorithmHeaderValue(AlgorithmIdentifiers.RSA_USING_SHA256);
        jws.setKeyIdHeaderValue(signingJwk.getKeyId());
        jws.setKey(signingJwk.getPrivateKey());
        jws.setPayload(claims.toJson());
        return jws.getCompactSerialization();
    }

    private String 만료된_서명_ID_token(RsaJsonWebKey signingJwk, String audience, String subject) throws Exception {
        JwtClaims claims = new JwtClaims();
        claims.setIssuer("https://appleid.apple.com");
        claims.setAudience(audience);
        claims.setSubject(subject);
        claims.setIssuedAtToNow();
        claims.setExpirationTimeMinutesInTheFuture(-1);

        JsonWebSignature jws = new JsonWebSignature();
        jws.setAlgorithmHeaderValue(AlgorithmIdentifiers.RSA_USING_SHA256);
        jws.setKeyIdHeaderValue(signingJwk.getKeyId());
        jws.setKey(signingJwk.getPrivateKey());
        jws.setPayload(claims.toJson());
        return jws.getCompactSerialization();
    }
}
