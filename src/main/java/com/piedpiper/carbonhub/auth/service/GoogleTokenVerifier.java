package com.piedpiper.carbonhub.auth.service;

import com.piedpiper.carbonhub.auth.models.dtos.GoogleClaims;

import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.jwk.source.RemoteJWKSet;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jose.util.DefaultResourceRetriever;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.proc.DefaultJWTClaimsVerifier;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import com.nimbusds.jwt.proc.JWTProcessor;
import com.piedpiper.carbonhub.exceptions.ApiException;
import com.nimbusds.jose.JWSAlgorithm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Set;

@Component
public class GoogleTokenVerifier {

    private static final Set<String> EMISORES_VALIDOS =
            Set.of("https://accounts.google.com", "accounts.google.com");

    private final JWTProcessor<SecurityContext> processor;
    private final String clientId;

    public GoogleTokenVerifier(
            @Value("${security.google.jwks-uri}") String jwksUri,
            @Value("${security.google.client-id}") String clientId,
            @Value("${security.google.timeout-ms}") int timeoutMs) throws MalformedURLException {
        this.clientId = clientId;
        DefaultResourceRetriever retriever = new DefaultResourceRetriever(timeoutMs, timeoutMs);
        JWKSource<SecurityContext> jwkSource = new RemoteJWKSet<>(new URL(jwksUri), retriever);
        DefaultJWTProcessor<SecurityContext> jwtProcessor = new DefaultJWTProcessor<>();
        jwtProcessor.setJWSKeySelector(
                new JWSVerificationKeySelector<>(JWSAlgorithm.RS256, jwkSource));
        jwtProcessor.setJWTClaimsSetVerifier(new DefaultJWTClaimsVerifier<>(
                clientId,
                new JWTClaimsSet.Builder().build(),
                Set.of("sub", "email", "iss", "aud", "exp")));
        this.processor = jwtProcessor;
    }

    public GoogleClaims verificar(String idToken) {
        JWTClaimsSet claims;
        try {
            claims = processor.process(idToken, null);
        } catch (Exception e) {
            if (esTimeout(e)) {
                throw ApiException.googleTimeout();
            }
            throw ApiException.tokenInvalido();
        }

        String issuer = claims.getIssuer();
        if (issuer == null || !EMISORES_VALIDOS.contains(issuer)) {
            throw ApiException.tokenInvalido();
        }
        List<String> audiences = claims.getAudience();
        if (audiences == null || !audiences.contains(clientId)) {
            throw ApiException.tokenInvalido();
        }

        try {
            String sub = claims.getStringClaim("sub");
            String email = claims.getStringClaim("email");
            Boolean emailVerified = claims.getBooleanClaim("email_verified");
            String name = claims.getStringClaim("name");
            String givenName = claims.getStringClaim("given_name");
            String familyName = claims.getStringClaim("family_name");
            if (sub == null || email == null) {
                throw ApiException.tokenInvalido();
            }
            return new GoogleClaims(sub, email, Boolean.TRUE.equals(emailVerified), name,
                    givenName, familyName);
        } catch (java.text.ParseException e) {
            throw ApiException.tokenInvalido();
        }
    }

    private boolean esTimeout(Throwable e) {
        Throwable actual = e;
        while (actual != null) {
            if (actual instanceof java.net.SocketTimeoutException) {
                return true;
            }
            actual = actual.getCause();
        }
        return false;
    }
}
