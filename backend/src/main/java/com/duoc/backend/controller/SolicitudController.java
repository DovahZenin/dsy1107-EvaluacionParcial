package com.duoc.backend.controller;

import com.duoc.backend.model.Solicitud;
import com.duoc.backend.service.SolicitudService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/solicitudes")
public class SolicitudController {
    private final SolicitudService service;

    public SolicitudController(SolicitudService service) {
        this.service = service;
    }

    // --- ENDPOINTS PARA EL SOLICITANTE ---

    @GetMapping("/mis-solicitudes")
    @PreAuthorize("hasAuthority('SCOPE_solicitudes/read')")
    public List<Solicitud> getMisSolicitudes(@AuthenticationPrincipal Jwt jwt) {
        // Extraemos el ID del usuario directamente del token (claim "sub")
        return service.obtenerPorUsuario(jwt.getSubject());
    }

    @PostMapping
    @PreAuthorize("hasAuthority('SCOPE_solicitudes/write')")
    public Solicitud crearSolicitud(@RequestBody Solicitud solicitud, @AuthenticationPrincipal Jwt jwt) {
        return service.crearSolicitud(solicitud, jwt.getSubject());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('SCOPE_solicitudes/write')")
    public void eliminarSolicitud(@PathVariable Long id) {
        service.eliminarSolicitud(id);
    }

    // --- ENDPOINTS PARA EL APROBADOR ---

    @GetMapping("/todas")
    @PreAuthorize("hasAuthority('SCOPE_solicitudes/approve')")
    public List<Solicitud> getTodasSolicitudes() {
        return service.obtenerTodas();
    }

    @PatchMapping("/{id}/evaluar")
    @PreAuthorize("hasAuthority('SCOPE_solicitudes/approve')")
    public Solicitud evaluarSolicitud(@PathVariable Long id, @RequestBody Map<String, String> payload) {
        String estado = payload.get("estado"); // "APROBADA" o "RECHAZADA"
        String comentario = payload.get("comentario");
        return service.evaluarSolicitud(id, estado, comentario);
    }
}