package com.duoc.backend.security;

import java.util.List;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

public class AudienceValidator implements OAuth2TokenValidator<Jwt> {

    private static final OAuth2Error ERROR = new OAuth2Error(
            OAuth2ErrorCodes.INVALID_TOKEN,
            "El token no contiene el audience requerido",
            null);

    private final String audience;

    public AudienceValidator(String audience) {
        this.audience = audience;
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt token) {
        // Cognito: el ID token lleva "aud" (el client id), pero el ACCESS token
        // (el que el frontend manda a la API) no trae "aud" sino "client_id".
        // Se acepta cualquiera de los dos: el valor esperado es el mismo client id.
        List<String> aud = token.getAudience();
        if (aud != null && aud.contains(audience)) {
            return OAuth2TokenValidatorResult.success();
        }
        String clientId = token.getClaimAsString("client_id");
        return audience.equals(clientId)
                ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(ERROR);
    }
}