package com.duoc.backend.repository;

import com.duoc.backend.model.Solicitud;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SolicitudRepository extends JpaRepository<Solicitud, Long> {
    List<Solicitud> findByUsuarioId(String usuarioId);
}