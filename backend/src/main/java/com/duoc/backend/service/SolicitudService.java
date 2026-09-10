package com.duoc.backend.service;

import com.duoc.backend.dto.SolicitudRequestDTO;
import com.duoc.backend.dto.SolicitudResponseDTO;
import com.duoc.backend.exception.BadRequestException;
import com.duoc.backend.exception.ResourceNotFoundException;
import com.duoc.backend.model.EstadoSolicitud;
import com.duoc.backend.model.Solicitud;
import com.duoc.backend.repository.SolicitudRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SolicitudService {

    private final SolicitudRepository solicitudRepository;

    public SolicitudService(SolicitudRepository solicitudRepository) {
        this.solicitudRepository = solicitudRepository;
    }

    @Transactional
    public SolicitudResponseDTO crearSolicitud(SolicitudRequestDTO request, String solicitanteId) {
        validarFechasYCampos(request);

        Solicitud solicitud = new Solicitud();
        solicitud.setSolicitanteId(solicitanteId);
        solicitud.setTipo(request.getTipo());
        solicitud.setDescripcion(request.getDescripcion());
        solicitud.setFechaInicio(request.getFechaInicio());
        solicitud.setFechaFin(request.getFechaFin());
        solicitud.setEstado(EstadoSolicitud.PENDIENTE);

        return SolicitudResponseDTO.fromEntity(solicitudRepository.save(solicitud));
    }

    public List<SolicitudResponseDTO> obtenerSolicitudesPorSolicitante(String solicitanteId) {
        return solicitudRepository.findBySolicitanteId(solicitanteId)
                .stream()
                .map(SolicitudResponseDTO::fromEntity)
                .collect(Collectors.toList());
    }

    public SolicitudResponseDTO obtenerPorId(Long id) {
        return SolicitudResponseDTO.fromEntity(buscarPorId(id));
    }

    @Transactional
    public SolicitudResponseDTO editarSolicitud(Long id, SolicitudRequestDTO request, String solicitanteId) {
        Solicitud solicitud = buscarPorId(id);

        if (!solicitud.getSolicitanteId().equals(solicitanteId)) {
            throw new BadRequestException("No tiene permisos para modificar esta solicitud.");
        }

        if (solicitud.getEstado() != EstadoSolicitud.PENDIENTE) {
            throw new BadRequestException("Solo se pueden modificar solicitudes en estado PENDIENTE.");
        }

        validarFechasYCampos(request);

        solicitud.setTipo(request.getTipo());
        solicitud.setDescripcion(request.getDescripcion());
        solicitud.setFechaInicio(request.getFechaInicio());
        solicitud.setFechaFin(request.getFechaFin());

        return SolicitudResponseDTO.fromEntity(solicitudRepository.save(solicitud));
    }

    @Transactional
    public void eliminarSolicitud(Long id, String solicitanteId) {
        Solicitud solicitud = buscarPorId(id);

        if (!solicitud.getSolicitanteId().equals(solicitanteId)) {
            throw new BadRequestException("No tiene permisos para eliminar esta solicitud.");
        }

        if (solicitud.getEstado() != EstadoSolicitud.PENDIENTE) {
            throw new BadRequestException("Solo se pueden eliminar solicitudes en estado PENDIENTE.");
        }

        solicitudRepository.delete(solicitud);
    }

    public List<SolicitudResponseDTO> obtenerPendientes() {
        return solicitudRepository.findByEstado(EstadoSolicitud.PENDIENTE)
                .stream()
                .map(SolicitudResponseDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public SolicitudResponseDTO aprobarSolicitud(Long id, String comentario) {
        Solicitud solicitud = buscarPorId(id);

        if (solicitud.getEstado() != EstadoSolicitud.PENDIENTE) {
            throw new BadRequestException("La solicitud ya ha sido procesada previamente.");
        }

        solicitud.setEstado(EstadoSolicitud.APROBADA);
        solicitud.setComentarioAprobador(comentario);
        solicitud.setFechaRespuesta(LocalDateTime.now());

        return SolicitudResponseDTO.fromEntity(solicitudRepository.save(solicitud));
    }

    @Transactional
    public SolicitudResponseDTO rechazarSolicitud(Long id, String comentario) {
        Solicitud solicitud = buscarPorId(id);

        if (solicitud.getEstado() != EstadoSolicitud.PENDIENTE) {
            throw new BadRequestException("La solicitud ya ha sido procesada previamente.");
        }

        solicitud.setEstado(EstadoSolicitud.RECHAZADA);
        solicitud.setComentarioAprobador(comentario);
        solicitud.setFechaRespuesta(LocalDateTime.now());

        return SolicitudResponseDTO.fromEntity(solicitudRepository.save(solicitud));
    }

    private Solicitud buscarPorId(Long id) {
        return solicitudRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Solicitud no encontrada con ID: " + id));
    }

    private void validarFechasYCampos(SolicitudRequestDTO request) {
        if (request.getTipo() == null) {
            throw new BadRequestException("El tipo de solicitud es obligatorio.");
        }
        if (request.getDescripcion() == null || request.getDescripcion().trim().isEmpty()) {
            throw new BadRequestException("La descripción es obligatoria.");
        }
        if (request.getFechaInicio() == null || request.getFechaFin() == null) {
            throw new BadRequestException("Las fechas de inicio y fin son obligatorias.");
        }
        if (request.getFechaFin().isBefore(request.getFechaInicio())) {
            throw new BadRequestException("La fecha de fin no puede ser anterior a la fecha de inicio.");
        }
    }
}