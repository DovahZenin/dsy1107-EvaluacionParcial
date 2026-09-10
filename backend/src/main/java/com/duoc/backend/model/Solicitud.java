package com.duoc.backend.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;;
@Getter 
@Setter 
@Entity
@Table(name = "solicitudes")
public class Solicitud {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String tipo; // Ej: "Vacaciones", "Permiso"
    private String descripcion;
    
    // Estados: PENDIENTE, APROBADA, RECHAZADA
    private String estado = "PENDIENTE"; 
    
    private String comentarioAprobador;
    private String usuarioId; // Identificador del solicitante
    private LocalDateTime fechaCreacion = LocalDateTime.now();

    // Generar Getters y Setters
}