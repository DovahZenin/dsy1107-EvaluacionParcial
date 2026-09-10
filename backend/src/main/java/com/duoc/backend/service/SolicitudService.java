package com.duoc.backend.service;

import com.duoc.backend.model.Solicitud;
import com.duoc.backend.repository.SolicitudRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class SolicitudService {
    private final SolicitudRepository repository;

    public SolicitudService(SolicitudRepository repository) {
        this.repository = repository;
    }

    // --- Operaciones del Solicitante ---
    
    public Solicitud crearSolicitud(Solicitud solicitud, String usuarioId) {
        solicitud.setUsuarioId(usuarioId);
        // Asignación estricta usando el Enum
        solicitud.setEstado(Solicitud.EstadoSolicitud.PENDIENTE);
        return repository.save(solicitud);
    }

    public List<Solicitud> obtenerPorUsuario(String usuarioId) {
        return repository.findByUsuarioId(usuarioId);
    }

    public void eliminarSolicitud(Long id) {
        repository.deleteById(id);
    }

    // --- Operaciones del Aprobador ---
    
    public List<Solicitud> obtenerTodas() {
        return repository.findAll();
    }

    public Solicitud evaluarSolicitud(Long id, String nuevoEstado, String comentario) {
        Optional<Solicitud> opt = repository.findById(id);
        if (opt.isPresent()) {
            Solicitud solicitud = opt.get();
            
            // Convierte el texto ("APROBADA" o "RECHAZADA") al Enum de forma segura
            // toUpperCase() evita errores si el frontend envía "aprobada" en minúsculas
            solicitud.setEstado(Solicitud.EstadoSolicitud.valueOf(nuevoEstado.toUpperCase()));
            
            solicitud.setComentarioAprobador(comentario);
            return repository.save(solicitud);
        }
        throw new RuntimeException("Solicitud no encontrada");
    }
}