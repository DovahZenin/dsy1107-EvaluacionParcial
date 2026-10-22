package com.duoc.notificaciones.model;

import com.duoc.notificaciones.config.MensajeriaProperties;
import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.function.Consumer;

/**
 * Politica unica de confirmacion de mensajes para TODOS los consumers:
 *
 *   exito                -> basicAck
 *   error NoReintentable -> basicNack(requeue=false) -> DLQ (sin reintentar)
 *   error transitorio    -> N reintentos en memoria; si se agotan,
 *                           basicNack(requeue=false) -> DLQ
 *
 * Los N reintentos estan en "mensajeria.reintentos" del application.yml.
 * basicNack con requeue=false entrega el mensaje al dead-letter exchange
 * declarado en la topologia, que lo deja en la DLQ de su cola.
 */
@Component
public class ManejadorDeAck {

    private static final Logger log = LoggerFactory.getLogger(ManejadorDeAck.class);

    private final MensajeriaProperties props;

    public ManejadorDeAck(MensajeriaProperties props) {
        this.props = props;
    }

    public void procesar(MensajeSolicitud mensaje, Channel channel, long tag, String origen,
                         Consumer<MensajeSolicitud> logicaDeNegocio) {
        int reintentos = 0;
        while (true) {
            try {
                logicaDeNegocio.accept(mensaje);
                channel.basicAck(tag, false);
                return;
            } catch (NoReintentableException e) {
                log.error("[{}] mensaje de solicitud #{} descartado a DLQ (no reintentable): {}",
                        origen, mensaje.solicitudId(), e.getMessage());
                nackHaciaDlq(channel, tag);
                return;
            } catch (Exception e) {
                reintentos++;
                if (reintentos >= props.getReintentos()) {
                    log.error("[{}] agotados {}/{} reintentos para solicitud #{} -> DLQ: {}",
                            origen, reintentos, props.getReintentos(), mensaje.solicitudId(), e.getMessage());
                    nackHaciaDlq(channel, tag);
                    return;
                }
                log.warn("[{}] intento {}/{} fallo para solicitud #{}: {}",
                        origen, reintentos, props.getReintentos(), mensaje.solicitudId(), e.getMessage());
            }
        }
    }

    private void nackHaciaDlq(Channel channel, long tag) {
        try {
            channel.basicNack(tag, false, false);
        } catch (IOException e) {
            log.error("no se pudo hacer basicNack; el broker descartara el mensaje", e);
        }
    }
}
