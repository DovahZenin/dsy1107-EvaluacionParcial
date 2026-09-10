package com.duoc.backend.repository;

import com.duoc.backend.model.EstadoSolicitud;
import com.duoc.backend.model.Solicitud;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SolicitudRepository extends JpaRepository<Solicitud, Long> {

    // Obtener todas las solicitudes de un solicitante específico
    List<Solicitud> findBySolicitanteId(String solicitanteId);

    // Obtener todas las solicitudes pendientes para la vista del Aprobador
    List<Solicitud> findByEstado(EstadoSolicitud estado);
}