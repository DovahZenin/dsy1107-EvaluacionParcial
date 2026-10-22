package com.duoc.backend.eventos;

/**
 * Contrato del mensaje publicado al broker. El notificaciones-service tiene un
 * record identico (su MensajeSolicitud): este es el que viaja en JSON.
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
