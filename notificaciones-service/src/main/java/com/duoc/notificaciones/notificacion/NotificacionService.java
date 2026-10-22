package com.duoc.notificaciones.notificacion;

import com.duoc.notificaciones.model.MensajeSolicitud;
import com.duoc.notificaciones.model.NoReintentableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Logica de negocio de las notificaciones (aqui simulada: solo registra).
 * No conoce RabbitMQ: recibe el mensaje ya deserializado y decide si puede
 * procesarlo o no.
 */
@Service
public class NotificacionService {

    private static final Logger log = LoggerFactory.getLogger(NotificacionService.class);

    public void registrarCreacion(MensajeSolicitud mensaje) {
        validarPayload(mensaje);
        log.info("[notificacion] solicitud #{} ({}) creada por {}",
                mensaje.solicitudId(), mensaje.tipo(), mensaje.usuarioId());
    }

    public void registrarEvaluacion(MensajeSolicitud mensaje) {
        validarPayload(mensaje);
        log.info("[notificacion] solicitud #{} ({}) evaluada a {} por {}",
                mensaje.solicitudId(), mensaje.tipo(), mensaje.estado(), mensaje.usuarioId());
    }

    /** Un payload POISON no tiene remedio: mejor a la DLQ que reintentar. */
    private void validarPayload(MensajeSolicitud mensaje) {
        if (mensaje.solicitudId() == null) {
            throw new NoReintentableException("el mensaje no trae solicitudId");
        }
        if (mensaje.descripcion() != null && mensaje.descripcion().contains("POISON")) {
            throw new NoReintentableException(
                    "payload POISON detectado en solicitud #" + mensaje.solicitudId());
        }
    }
}
