package com.duoc.adminrabbit.error;

import java.time.Instant;
import java.util.Map;

/** Respuesta de error consistente para toda la API administradora. */
public record ApiError(
        Instant timestamp,
        int status,
        String error,
        String mensaje,
        Map<String, String> campos) {

    public static ApiError de(int status, String error, String mensaje) {
        return new ApiError(Instant.now(), status, error, mensaje, null);
    }

    public static ApiError conCampos(int status, String error, String mensaje, Map<String, String> campos) {
        return new ApiError(Instant.now(), status, error, mensaje, campos);
    }
}
