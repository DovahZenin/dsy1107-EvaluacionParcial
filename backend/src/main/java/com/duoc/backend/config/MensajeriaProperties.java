package com.duoc.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Todos los nombres de colas, exchanges y bindings del sistema, leidos desde
 * el bloque "mensajeria" del application.yml. Es la unica fuente de nombres:
 * ni controllers ni servicios conocen un nombre de cola escrito a mano.
 */
@ConfigurationProperties(prefix = MensajeriaProperties.PREFIJO)
public class MensajeriaProperties {

    public static final String PREFIJO = "mensajeria";

    private String exchangeSolicitudes;
    private String exchangeMuerta;
    private int reintentos = 3;
    private Map<String, Ruta> solicitudes = new LinkedHashMap<>();

    /** Una ruta de mensajeria = un caso de uso: su cola, su dlq y su routing key. */
    public static class Ruta {
        private String routingKey;
        private String cola;
        private String dlq;

        public String getRoutingKey() {
            return routingKey;
        }

        public void setRoutingKey(String routingKey) {
            this.routingKey = routingKey;
        }

        public String getCola() {
            return cola;
        }

        public void setCola(String cola) {
            this.cola = cola;
        }

        public String getDlq() {
            return dlq;
        }

        public void setDlq(String dlq) {
            this.dlq = dlq;
        }
    }

    public String getExchangeSolicitudes() {
        return exchangeSolicitudes;
    }

    public void setExchangeSolicitudes(String exchangeSolicitudes) {
        this.exchangeSolicitudes = exchangeSolicitudes;
    }

    public String getExchangeMuerta() {
        return exchangeMuerta;
    }

    public void setExchangeMuerta(String exchangeMuerta) {
        this.exchangeMuerta = exchangeMuerta;
    }

    public int getReintentos() {
        return reintentos;
    }

    public void setReintentos(int reintentos) {
        this.reintentos = reintentos;
    }

    public Map<String, Ruta> getSolicitudes() {
        return solicitudes;
    }

    public void setSolicitudes(Map<String, Ruta> solicitudes) {
        this.solicitudes = solicitudes;
    }

    /** Colas que pertenecen al sistema y no se borran por API. */
    public java.util.Set<String> nombresDeColasDelSistema() {
        java.util.Set<String> nombres = new java.util.LinkedHashSet<>();
        solicitudes.values().forEach(r -> {
            nombres.add(r.getCola());
            nombres.add(r.getDlq());
        });
        return nombres;
    }

    /** Exchanges que pertenecen al sistema y no se borran por API. */
    public java.util.Set<String> nombresDeExchangesDelSistema() {
        return java.util.Set.of(exchangeSolicitudes, exchangeMuerta);
    }
}
