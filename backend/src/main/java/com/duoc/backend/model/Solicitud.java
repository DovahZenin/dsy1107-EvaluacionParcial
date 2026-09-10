package com.duoc.backend.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter 
@Setter 
@Entity
@Table(name = "solicitudes")
public class Solicitud {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    // El tipo de solicitud forzado a opciones categóricas
    @Enumerated(EnumType.STRING)
    private TipoSolicitud tipo;
    
    private String descripcion;
    
    @Enumerated(EnumType.STRING)
    private EstadoSolicitud estado = EstadoSolicitud.PENDIENTE; 
    
    private String comentarioAprobador;
    private String usuarioId; 
    private LocalDateTime fechaCreacion = LocalDateTime.now();

    // Categorías disponibles para el solicitante
    public enum TipoSolicitud {
        VACACIONES,
        PERMISO_ADMINISTRATIVO,
        LICENCIA_MEDICA,
        TRABAJO_REMOTO
    }

    // Estados de evaluación
    public enum EstadoSolicitud {
        PENDIENTE,
        APROBADA,
        RECHAZADA
    }
}