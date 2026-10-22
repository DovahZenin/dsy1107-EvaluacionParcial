package com.duoc.notificaciones.notificacion;

import com.duoc.notificaciones.model.ManejadorDeAck;
import com.duoc.notificaciones.model.MensajeSolicitud;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

/**
 * Dominio funcional: NOTIFICACION.
 * Consume la cola de "solicitud.creada" con ACK manual (ManejadorDeAck).
 */
@Component
public class NotificacionSolicitudCreadaConsumer {

    private final ManejadorDeAck manejador;
    private final NotificacionService notificacionService;

    public NotificacionSolicitudCreadaConsumer(ManejadorDeAck manejador, NotificacionService notificacionService) {
        this.manejador = manejador;
        this.notificacionService = notificacionService;
    }

    @RabbitListener(queues = "${mensajeria.solicitudes.creada.cola}")
    public void onSolicitudCreada(MensajeSolicitud mensaje, Channel channel,
                                  @Header(AmqpHeaders.DELIVERY_TAG) long tag) {
        manejador.procesar(mensaje, channel, tag, "notificacion/creada",
                notificacionService::registrarCreacion);
    }
}
