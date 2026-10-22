package com.duoc.notificaciones.auditoria;

import com.duoc.notificaciones.model.MensajeSolicitud;
import com.duoc.notificaciones.model.NoReintentableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Logica de negocio de auditoria (simulada: registra el evento).
 * Recibe TODAS las solicitudes ("solicitud.*"), creadas o evaluadas.
 */
@Service
public class AuditoriaService {

    private static final Logger log = LoggerFactory.getLogger(AuditoriaService.class);

    public void registrarEvento(MensajeSolicitud mensaje) {
        if (mensaje.solicitudId() == null) {
            throw new NoReintentableException("el mensaje no trae solicitudId");
        }
        if (mensaje.descripcion() != null && mensaje.descripcion().contains("POISON")) {
            throw new NoReintentableException(
                    "payload POISON detectado en solicitud #" + mensaje.solicitudId());
        }
        log.info("[auditoria] evento {} de solicitud #{} (estado {}, usuario {})",
                mensaje.evento(), mensaje.solicitudId(), mensaje.estado(), mensaje.usuarioId());
    }
}
