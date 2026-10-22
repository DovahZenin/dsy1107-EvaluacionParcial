package com.duoc.notificaciones.auditoria;

import com.duoc.notificaciones.model.ManejadorDeAck;
import com.duoc.notificaciones.model.MensajeSolicitud;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

/**
 * Dominio funcional: AUDITORIA.
 * Consume TODOS los eventos de solicitudes ("solicitud.*") con ACK manual.
 */
@Component
public class AuditoriaSolicitudConsumer {

    private final ManejadorDeAck manejador;
    private final AuditoriaService auditoriaService;

    public AuditoriaSolicitudConsumer(ManejadorDeAck manejador, AuditoriaService auditoriaService) {
        this.manejador = manejador;
        this.auditoriaService = auditoriaService;
    }

    @RabbitListener(queues = "${mensajeria.solicitudes.auditoria.cola}")
    public void onSolicitud(MensajeSolicitud mensaje, Channel channel,
                            @Header(AmqpHeaders.DELIVERY_TAG) long tag) {
        manejador.procesar(mensaje, channel, tag, "auditoria", auditoriaService::registrarEvento);
    }
}
