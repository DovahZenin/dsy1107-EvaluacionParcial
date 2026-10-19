package com.duoc.backend.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(authz -> authz
                .requestMatchers("/actuator/health").permitAll()
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2.jwt()); // Usa el validador nativo de scopes
        return http.build();
    }

    // Firma + expiracion + issuer + audience en TODAS las peticiones que lleguen
    // a cualquier endpoint, incluso directo por la IP de la task.
    @Bean
    public JwtDecoder jwtDecoder(
            @Value("${app.oauth.issuer-uri}") String issuerUri,
            @Value("${app.oauth.audience}") String audience) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder
                .withJwkSetUri(issuerUri + "/.well-known/jwks.json")
                .build();
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<Jwt>(
                JwtValidators.createDefaultWithIssuer(issuerUri),
                new AudienceValidator(audience)));
        return decoder;
    }
}