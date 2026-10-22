package com.duoc.backend.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AudienceValidatorTest {

    private final AudienceValidator validator = new AudienceValidator("client-esperado");

    private Jwt jwt(List<String> audience) {
        return Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject("usuario-1")
                .audience(audience)
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(300))
                .build();
    }

    @Test
    void validaCuandoLaAudienceEsLaEsperada() {
        assertFalse(validator.validate(jwt(List.of("client-esperado"))).hasErrors());
    }

    @Test
    void validaCuandoLaAudienceContieneAdemasOtras() {
        assertFalse(validator.validate(jwt(List.of("otro-client", "client-esperado"))).hasErrors());
    }

    @Test
    void rechazaCuandoLaAudienceEsDistinta() {
        assertTrue(validator.validate(jwt(List.of("client-ajeno"))).hasErrors());
    }

    @Test
    void rechazaCuandoLaAudienceEstaVacia() {
        assertTrue(validator.validate(jwt(List.of())).hasErrors());
    }

    // Cognito: el access token no trae "aud" sino el claim "client_id".
    // Se valida contra ese claim cuando no hay "aud".
    private Jwt accessToken(String clientId) {
        return Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject("usuario-1")
                .claim("client_id", clientId)
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(300))
                .build();
    }

    @Test
    void validaAccessTokenConClientIdEsperado() {
        assertFalse(validator.validate(accessToken("client-esperado")).hasErrors());
    }

    @Test
    void rechazaAccessTokenConClientIdAjeno() {
        assertTrue(validator.validate(accessToken("client-ajeno")).hasErrors());
    }
}