package com.duoc.backend.dto;

import com.duoc.backend.model.EstadoSolicitud;
import com.duoc.backend.model.Solicitud;
import com.duoc.backend.model.TipoSolicitud;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class SolicitudResponseDTO {

    private Long id;
    private String solicitanteId;
    private TipoSolicitud tipo;
    private String descripcion;
    private LocalDate fechaInicio;
    private LocalDate fechaFin;
    private EstadoSolicitud estado;
    private String comentarioAprobador;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaRespuesta;

    public SolicitudResponseDTO() {
    }

    public static SolicitudResponseDTO fromEntity(Solicitud solicitud) {
        SolicitudResponseDTO dto = new SolicitudResponseDTO();
        dto.setId(solicitud.getId());
        dto.setSolicitanteId(solicitud.getSolicitanteId());
        dto.setTipo(solicitud.getTipo());
        dto.setDescripcion(solicitud.getDescripcion());
        dto.setFechaInicio(solicitud.getFechaInicio());
        dto.setFechaFin(solicitud.getFechaFin());
        dto.setEstado(solicitud.getEstado());
        dto.setComentarioAprobador(solicitud.getComentarioAprobador());
        dto.setFechaCreacion(solicitud.getFechaCreacion());
        dto.setFechaRespuesta(solicitud.getFechaRespuesta());
        return dto;
    }

    // Getters y Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSolicitanteId() {
        return solicitanteId;
    }

    public void setSolicitanteId(String solicitanteId) {
        this.solicitanteId = solicitanteId;
    }

    public TipoSolicitud getTipo() {
        return tipo;
    }

    public void setTipo(TipoSolicitud tipo) {
        this.tipo = tipo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public LocalDate getFechaInicio() {
        return fechaInicio;
    }

    public void setFechaInicio(LocalDate fechaInicio) {
        this.fechaInicio = fechaInicio;
    }

    public LocalDate getFechaFin() {
        return fechaFin;
    }

    public void setFechaFin(LocalDate fechaFin) {
        this.fechaFin = fechaFin;
    }

    public EstadoSolicitud getEstado() {
        return estado;
    }

    public void setEstado(EstadoSolicitud estado) {
        this.estado = estado;
    }

    public String getComentarioAprobador() {
        return comentarioAprobador;
    }

    public void setComentarioAprobador(String comentarioAprobador) {
        this.comentarioAprobador = comentarioAprobador;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(LocalDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public LocalDateTime getFechaRespuesta() {
        return fechaRespuesta;
    }

    public void setFechaRespuesta(LocalDateTime fechaRespuesta) {
        this.fechaRespuesta = fechaRespuesta;
    }
}