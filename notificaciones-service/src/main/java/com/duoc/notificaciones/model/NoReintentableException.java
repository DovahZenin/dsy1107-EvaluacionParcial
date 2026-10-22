package com.duoc.notificaciones.model;

/**
 * Error que NO se reintenta: el mensaje esta mal formado o es deliberadamente
 * invalido (payload "POISON"). Se NACKea sin requeue y cae directo a la DLQ,
 * sin gastar los reintentos.
 */
public class NoReintentableException extends RuntimeException {

    public NoReintentableException(String mensaje) {
        super(mensaje);
    }
}
