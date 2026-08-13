package com.fmi.domain.auth.service.apple;

import org.jose4j.jws.AlgorithmIdentifiers;
import org.jose4j.jws.JsonWebSignature;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.lang.JoseException;

import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.GeneralSecurityException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

public class AppleClientSecretGenerator {

    private static final String APPLE_AUDIENCE = "https://appleid.apple.com";
    private static final float CLIENT_SECRET_VALIDITY_MINUTES = 60 * 24 * 180;

    private final String teamId;
    private final String clientId;
    private final String keyId;
    private final String privateKeyBase64;

    public AppleClientSecretGenerator(String teamId, String clientId, String keyId, String privateKeyBase64) {
        this.teamId = teamId;
        this.clientId = clientId;
        this.keyId = keyId;
        this.privateKeyBase64 = privateKeyBase64;
    }

    public String generate() {
        try {
            JwtClaims claims = new JwtClaims();
            claims.setIssuer(teamId);
            claims.setSubject(clientId);
            claims.setAudience(APPLE_AUDIENCE);
            claims.setIssuedAtToNow();
            claims.setExpirationTimeMinutesInTheFuture(CLIENT_SECRET_VALIDITY_MINUTES);

            JsonWebSignature jws = new JsonWebSignature();
            jws.setAlgorithmHeaderValue(AlgorithmIdentifiers.ECDSA_USING_P256_CURVE_AND_SHA256);
            jws.setKeyIdHeaderValue(keyId);
            jws.setPayload(claims.toJson());
            jws.setKey(privateKey());
            return jws.getCompactSerialization();
        } catch (JoseException | GeneralSecurityException | IllegalArgumentException exception) {
            throw new IllegalStateException("Apple client secret 생성에 실패했습니다.", exception);
        }
    }

    private PrivateKey privateKey() throws GeneralSecurityException {
        byte[] encoded = Base64.getDecoder().decode(privateKeyBase64);
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(encoded);
        return KeyFactory.getInstance("EC").generatePrivate(keySpec);
    }
}
