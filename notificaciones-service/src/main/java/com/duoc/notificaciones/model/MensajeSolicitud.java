package com.duoc.notificaciones.model;

/**
 * Contrato del mensaje que publica el backend al crear o evaluar una
 * solicitud. JSON en el broker (Jackson2JsonMessageConverter).
 */
public record MensajeSolicitud(
        String evento,
        Long solicitudId,
        String tipo,
        String estado,
        String usuarioId,
        String descripcion,
        String timestamp) {
}
