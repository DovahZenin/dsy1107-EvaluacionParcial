package com.duoc.backend.eventos;

import com.duoc.backend.config.MensajeriaProperties;
import com.duoc.backend.model.Solicitud;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * Publica los eventos de solicitudes en el exchange del sistema.
 *
 * Fire-and-forget con degradacion elegante: si el broker no esta disponible
 * (entorno local sin docker, ECS sin broker), la operacion de negocio NO se
 * rompe; solo se registra un warning. Los nombres de exchange y routing keys
 * salen de application.yml (MensajeriaProperties), nunca estan a mano aqui.
 */
@Component
public class SolicitudEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(SolicitudEventPublisher.class);

    private final RabbitTemplate rabbitTemplate;
    private final MensajeriaProperties props;

    public SolicitudEventPublisher(RabbitTemplate rabbitTemplate, MensajeriaProperties props) {
        this.rabbitTemplate = rabbitTemplate;
        this.props = props;
    }

    public void publicarCreada(Solicitud solicitud) {
        publicar(evento("creada", "solicitud.creada", solicitud));
    }

    public void publicarEvaluada(Solicitud solicitud) {
        publicar(evento("evaluada", "solicitud.evaluada", solicitud));
    }

    private MensajeSolicitud evento(String caso, String evento, Solicitud solicitud) {
        MensajeriaProperties.Ruta ruta = props.getSolicitudes().get(caso);
        return new MensajeSolicitud(
                ruta.getRoutingKey() != null ? ruta.getRoutingKey() : evento,
                solicitud.getId(),
                solicitud.getTipo() != null ? solicitud.getTipo().name() : null,
                solicitud.getEstado() != null ? solicitud.getEstado().name() : null,
                solicitud.getUsuarioId(),
                solicitud.getDescripcion(),
                solicitud.getFechaCreacion() != null ? solicitud.getFechaCreacion().toString() : null);
    }

    private void publicar(MensajeSolicitud mensaje) {
        try {
            rabbitTemplate.convertAndSend(props.getExchangeSolicitudes(), mensaje.evento(), mensaje);
            log.info("Evento {} publicado para la solicitud #{}", mensaje.evento(), mensaje.solicitudId());
        } catch (AmqpException e) {
            log.warn("No se pudo publicar el evento {} de la solicitud #{} (broker no disponible): {}",
                    mensaje.evento(), mensaje.solicitudId(), e.getMessage());
        }
    }
}
