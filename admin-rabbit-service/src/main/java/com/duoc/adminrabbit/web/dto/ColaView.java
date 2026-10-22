package com.duoc.adminrabbit.web.dto;

/** Vista de una cola: nombre, origen y estado en el broker. */
public record ColaView(
        String nombre,
        String origen,
        long mensajesPendientes,
        long consumidores,
        boolean existe) {
}
