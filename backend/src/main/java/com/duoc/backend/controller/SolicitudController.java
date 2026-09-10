package com.duoc.backend.controller;

import com.duoc.backend.dto.SolicitudRequestDTO;
import com.duoc.backend.dto.SolicitudResponseDTO;
import com.duoc.backend.service.SolicitudService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/solicitudes")
public class SolicitudController {

    private final SolicitudService solicitudService;

    public SolicitudController(SolicitudService solicitudService) {
        this.solicitudService = solicitudService;
    }

    @GetMapping
    public List<SolicitudResponseDTO> obtenerMisSolicitudes(@AuthenticationPrincipal Jwt jwt) {
        // Extrae el claim 'sub' o 'username' del token de Cognito
        String solicitanteId = jwt.getClaimAsString("sub"); 
        return solicitudService.obtenerSolicitudesPorSolicitante(solicitanteId);
    }

    @PostMapping
    public SolicitudResponseDTO crearSolicitud(@RequestBody SolicitudRequestDTO dto, @AuthenticationPrincipal Jwt jwt) {
        String solicitanteId = jwt.getClaimAsString("sub");
        return solicitudService.crearSolicitud(dto, solicitanteId);
    }
}