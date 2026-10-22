package com.duoc.adminrabbit.service;

import com.duoc.adminrabbit.config.MensajeriaProperties;
import com.duoc.adminrabbit.web.dto.ColaView;
import com.duoc.adminrabbit.web.dto.ExchangeView;
import com.duoc.adminrabbit.web.dto.NuevaColaRequest;
import com.duoc.adminrabbit.web.dto.NuevaExchangeRequest;
import com.duoc.adminrabbit.web.dto.TipoExchange;
import com.rabbitmq.client.AMQP;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.HeadersExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * Unica puerta a RabbitMQ para operar la infraestructura: crear, listar y
 * borrar colas y exchanges. Los controllers llaman a estos metodos de alto
 * nivel y no tocan AmqpAdmin ni conocen argumentos de dead-lettering.
 *
 * Reglas de la casa:
 *  - Las colas/exchanges definidos en application.yml son del SISTEMA: se
 *    listan pero no se borran por API (409). Los dinamicos, si.
 *  - Toda cola dinamica nace con DLQ y dead-lettering hacia la DLX del
 *    sistema, igual que las colas de configuracion: el manejo de errores es
 *    el mismo en todo el broker.
 */
@Service
public class RabbitAdminService {

    private static final Logger log = LoggerFactory.getLogger(RabbitAdminService.class);

    private final AmqpAdmin amqpAdmin;
    private final RabbitTemplate rabbitTemplate;
    private final MensajeriaProperties props;

    /** Colas y exchanges creados por API en esta ejecucion (para poder listarlos). */
    private final Map<String, ColaView> colasDinamicas = new LinkedHashMap<>();
    private final Map<String, ExchangeView> exchangesDinamicos = new LinkedHashMap<>();

    public RabbitAdminService(AmqpAdmin amqpAdmin, RabbitTemplate rabbitTemplate, MensajeriaProperties props) {
        this.amqpAdmin = amqpAdmin;
        this.rabbitTemplate = rabbitTemplate;
        this.props = props;
    }

    // ------------------------------------------------------------------
    // Colas
    // ------------------------------------------------------------------

    public List<ColaView> listarColas() {
        List<ColaView> colas = new ArrayList<>();
        props.nombresDeColasDelSistema().stream()
                .sorted()
                .forEach(nombre -> colas.add(vistaDeCola(nombre, "sistema")));
        colasDinamicas.values().forEach(colas::add);
        return colas;
    }

    public ColaView obtenerCola(String nombre) {
        ColaView vista = vistaDeCola(nombre, "sistema");
        if (vista.existe()) {
            return vista;
        }
        ColaView dinamica = colasDinamicas.get(nombre);
        if (dinamica != null) {
            return vistaDeCola(nombre, "dinamica");
        }
        throw new NoSuchElementException("no existe la cola '" + nombre + "'");
    }

    public ColaView crearCola(NuevaColaRequest request) {
        String nombre = request.getNombre();

        if (props.nombresDeColasDelSistema().contains(nombre)) {
            throw new IllegalStateException(
                    "la cola '" + nombre + "' pertenece al sistema (application.yml) y no se puede crear por API");
        }
        if (existeCola(nombre)) {
            throw new IllegalStateException("la cola '" + nombre + "' ya existe");
        }

        String exchange = request.getExchange() != null && !request.getExchange().isBlank()
                ? request.getExchange()
                : props.getExchangeSolicitudes();
        String routingKey = request.getRoutingKey() != null && !request.getRoutingKey().isBlank()
                ? request.getRoutingKey()
                : nombre;

        verificarQueElExchangeExista(exchange);

        // Misma politica de errores que las colas del sistema: DLX + DLQ propia.
        String dlq = nombre + ".dlq";
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", props.getExchangeMuerta());
        args.put("x-dead-letter-routing-key", nombre);

        try {
            amqpAdmin.declareQueue(new Queue(nombre, true, false, false, args));
            amqpAdmin.declareQueue(new Queue(dlq, true));
            amqpAdmin.declareBinding(new Binding(nombre, Binding.DestinationType.QUEUE, exchange, routingKey, null));
            amqpAdmin.declareBinding(new Binding(dlq, Binding.DestinationType.QUEUE, props.getExchangeMuerta(), nombre, null));
        } catch (AmqpException e) {
            throw new IllegalStateException(
                    "el broker rechazo crear la cola '" + nombre + "': " + causaDeBroker(e), e);
        }

        log.info("Cola creada: {} -> {} ({}), dlq {}", nombre, exchange, routingKey, dlq);
        ColaView vista = vistaDeCola(nombre, "dinamica");
        colasDinamicas.put(nombre, vista);
        return vista;
    }

    public void eliminarCola(String nombre) {
        if (props.nombresDeColasDelSistema().contains(nombre)) {
            throw new IllegalStateException(
                    "la cola '" + nombre + "' pertenece al sistema (application.yml): borrala desde el archivo y reinicia");
        }
        if (!existeCola(nombre)) {
            throw new NoSuchElementException("no existe la cola '" + nombre + "'");
        }
        amqpAdmin.deleteQueue(nombre);
        colasDinamicas.remove(nombre);
        log.info("Cola eliminada: {}", nombre);
    }

    // ------------------------------------------------------------------
    // Exchanges
    // ------------------------------------------------------------------

    public List<ExchangeView> listarExchanges() {
        List<ExchangeView> exchanges = new ArrayList<>();
        exchanges.add(new ExchangeView(props.getExchangeSolicitudes(), "TOPIC", "sistema"));
        exchanges.add(new ExchangeView(props.getExchangeMuerta(), "DIRECT", "sistema"));
        exchangesDinamicos.values().forEach(exchanges::add);
        return exchanges;
    }

    public ExchangeView crearExchange(NuevaExchangeRequest request) {
        String nombre = request.getNombre();
        TipoExchange tipo = request.getTipo() != null ? request.getTipo() : TipoExchange.DIRECT;

        if (props.nombresDeExchangesDelSistema().contains(nombre)) {
            throw new IllegalStateException(
                    "el exchange '" + nombre + "' pertenece al sistema (application.yml) y no se puede crear por API");
        }
        if (existeExchange(nombre)) {
            throw new IllegalStateException("el exchange '" + nombre + "' ya existe");
        }

        Exchange exchange = construir(tipo, nombre);
        try {
            amqpAdmin.declareExchange(exchange);
        } catch (AmqpException e) {
            throw new IllegalStateException(
                    "el broker rechazo crear el exchange '" + nombre + "': " + causaDeBroker(e), e);
        }

        log.info("Exchange creado: {} ({})", nombre, tipo);
        ExchangeView vista = new ExchangeView(nombre, tipo.name(), "dinamica");
        exchangesDinamicos.put(nombre, vista);
        return vista;
    }

    public void eliminarExchange(String nombre) {
        if (props.nombresDeExchangesDelSistema().contains(nombre)) {
            throw new IllegalStateException(
                    "el exchange '" + nombre + "' pertenece al sistema (application.yml): borrala desde el archivo y reinicia");
        }
        if (!existeExchange(nombre)) {
            throw new NoSuchElementException("no existe el exchange '" + nombre + "'");
        }
        amqpAdmin.deleteExchange(nombre);
        exchangesDinamicos.remove(nombre);
        log.info("Exchange eliminado: {}", nombre);
    }

    // ------------------------------------------------------------------
    // Internos
    // ------------------------------------------------------------------

    private ColaView vistaDeCola(String nombre, String origen) {
        try {
            return rabbitTemplate.execute(channel -> {
                AMQP.Queue.DeclareOk ok = channel.queueDeclarePassive(nombre);
                return new ColaView(nombre, origen, ok.getMessageCount(), ok.getConsumerCount(), true);
            });
        } catch (AmqpException e) {
            // cola inexistente o broker caido: se reporta como no existente
            return new ColaView(nombre, origen, 0, 0, false);
        }
    }

    private boolean existeCola(String nombre) {
        return vistaDeCola(nombre, "dinamica").existe();
    }

    private boolean existeExchange(String nombre) {
        try {
            rabbitTemplate.execute(channel -> {
                channel.exchangeDeclarePassive(nombre);
                return null;
            });
            return true;
        } catch (AmqpException e) {
            return false;
        }
    }

    private void verificarQueElExchangeExista(String nombre) {
        if (!existeExchange(nombre)) {
            throw new IllegalStateException(
                    "no existe el exchange '" + nombre + "': crealo antes con POST /exchanges");
        }
    }

    private Exchange construir(TipoExchange tipo, String nombre) {
        return switch (tipo) {
            case DIRECT -> new DirectExchange(nombre, true, false);
            case TOPIC -> new TopicExchange(nombre, true, false);
            case FANOUT -> new FanoutExchange(nombre, true, false);
            case HEADERS -> new HeadersExchange(nombre, true, false);
        };
    }

    private String causaDeBroker(AmqpException e) {
        Throwable causa = e.getCause();
        if (causa instanceof IOException io && io.getMessage() != null) {
            return io.getMessage();
        }
        return e.getMessage();
    }
}
