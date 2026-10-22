package com.duoc.adminrabbit.web.dto;

/** Vista de un exchange: nombre, tipo y origen. */
public record ExchangeView(
        String nombre,
        String tipo,
        String origen) {
}
