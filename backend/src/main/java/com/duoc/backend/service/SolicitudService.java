package com.duoc.backend.service;

import com.duoc.backend.eventos.SolicitudEventPublisher;
import com.duoc.backend.model.Solicitud;
import com.duoc.backend.repository.SolicitudRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class SolicitudService {
    private final SolicitudRepository repository;
    private final SolicitudEventPublisher publisher;

    public SolicitudService(SolicitudRepository repository, SolicitudEventPublisher publisher) {
        this.repository = repository;
        this.publisher = publisher;
    }

    // --- Operaciones del Solicitante ---
    
    public Solicitud crearSolicitud(Solicitud solicitud, String usuarioId) {
        solicitud.setUsuarioId(usuarioId);
        // Asignación estricta usando el Enum
        solicitud.setEstado(Solicitud.EstadoSolicitud.PENDIENTE);
        Solicitud guardada = repository.save(solicitud);
        publisher.publicarCreada(guardada);
        return guardada;
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
            Solicitud evaluada = repository.save(solicitud);
            publisher.publicarEvaluada(evaluada);
            return evaluada;
        }
        throw new RuntimeException("Solicitud no encontrada");
    }
}