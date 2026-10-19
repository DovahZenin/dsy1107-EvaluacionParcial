package com.duoc.backend.controller;

import com.duoc.backend.model.Solicitud;
import com.duoc.backend.service.SolicitudService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SolicitudController.class)
class SolicitudControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @TestConfiguration
    @EnableMethodSecurity
    static class Stubs {
        @Bean
        JwtDecoder jwtDecoder() {
            return token -> {
                String scope = switch (token) {
                    case "token-aprobador" -> "solicitudes/read solicitudes/write solicitudes/approve";
                    case "token-solicitante" -> "solicitudes/read solicitudes/write";
                    default -> "";
                };
                return Jwt.withTokenValue(token)
                        .header("alg", "none")
                        .subject("usuario-1")
                        .claim("scope", scope)
                        .issuedAt(Instant.now())
                        .expiresAt(Instant.now().plusSeconds(300))
                        .build();
            };
        }

        @Bean
        @Primary
        SolicitudService solicitudServiceStub() {
            return new SolicitudService(null) {
                @Override
                public List<Solicitud> obtenerTodas() {
                    return List.of();
                }
            };
        }
    }

    @Test
    void sinToken_responde401() throws Exception {
        mockMvc.perform(get("/api/solicitudes/todas"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenSinScopeAprobador_responde403() throws Exception {
        mockMvc.perform(get("/api/solicitudes/todas")
                        .header("Authorization", "Bearer token-solicitante"))
                .andExpect(status().isForbidden());
    }

    @Test
    void tokenConScopeAprobador_responde200() throws Exception {
        mockMvc.perform(get("/api/solicitudes/todas")
                        .header("Authorization", "Bearer token-aprobador"))
                .andExpect(status().isOk());
    }
}
