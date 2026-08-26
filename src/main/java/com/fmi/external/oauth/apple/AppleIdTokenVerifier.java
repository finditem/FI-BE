package com.fmi.external.oauth.apple;

import com.fmi.global.apiPayload.code.status.ErrorStatus;
import com.fmi.global.apiPayload.exception.GeneralException;
import org.jose4j.jwa.AlgorithmConstraints;
import org.jose4j.jws.AlgorithmIdentifiers;
import org.jose4j.jwt.MalformedClaimException;
import org.jose4j.jwt.consumer.InvalidJwtException;
import org.jose4j.jwt.consumer.JwtConsumer;
import org.jose4j.jwt.consumer.JwtConsumerBuilder;
import org.jose4j.keys.resolvers.VerificationKeyResolver;

public class AppleIdTokenVerifier {

    private static final String APPLE_ISSUER = "https://appleid.apple.com";

    private final JwtConsumer jwtConsumer;

    public AppleIdTokenVerifier(String clientId, VerificationKeyResolver verificationKeyResolver) {
        this.jwtConsumer = new JwtConsumerBuilder()
                .setRequireExpirationTime()
                .setRequireSubject()
                .setExpectedIssuer(APPLE_ISSUER)
                .setExpectedAudience(clientId)
                .setVerificationKeyResolver(verificationKeyResolver)
                .setJwsAlgorithmConstraints(
                        AlgorithmConstraints.ConstraintType.PERMIT, AlgorithmIdentifiers.RSA_USING_SHA256)
                .build();
    }

    public String verifyAndGetSubject(String idToken) {
        try {
            return jwtConsumer.processToClaims(idToken).getSubject();
        } catch (InvalidJwtException | MalformedClaimException exception) {
            throw new GeneralException(ErrorStatus._APPLE_ID_TOKEN_INVALID);
        }
    }
}
