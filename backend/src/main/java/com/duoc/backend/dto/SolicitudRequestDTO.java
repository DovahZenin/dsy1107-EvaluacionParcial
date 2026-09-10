package com.duoc.backend.dto;

import com.duoc.backend.model.TipoSolicitud;
import java.time.LocalDate;

public class SolicitudRequestDTO {

    private TipoSolicitud tipo;
    private String descripcion;
    private LocalDate fechaInicio;
    private LocalDate fechaFin;

    public SolicitudRequestDTO() {
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
}