package com.duoc.adminrabbit.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Pedido para crear una cola dinamica con su DLQ y sus bindings.
 * exchange y routingKey son opcionales: si no vienen, se usa el exchange del
 * sistema (mensajeria.exchange-solicitudes) y el nombre de la cola como
 * routing key.
 */
public class NuevaColaRequest {

    @NotBlank(message = "el nombre de la cola es obligatorio")
    @Size(max = 255, message = "el nombre de la cola no puede superar 255 caracteres")
    @Pattern(
            regexp = "^[a-zA-Z0-9._-]+$",
            message = "el nombre solo puede contener letras, numeros, punto, guion bajo y guion (sin espacios)")
    private String nombre;

    @Size(max = 255, message = "el nombre del exchange no puede superar 255 caracteres")
    @Pattern(
            regexp = "^[a-zA-Z0-9._-]+$",
            message = "el exchange solo puede contener letras, numeros, punto, guion bajo y guion (sin espacios)")
    private String exchange;

    @Size(max = 255, message = "el routing key no puede superar 255 caracteres")
    @Pattern(
            regexp = "^[a-zA-Z0-9.*_-]+$",
            message = "el routing key solo puede contener letras, numeros, punto, asterisco, guion bajo y guion (sin espacios)")
    private String routingKey;

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getExchange() {
        return exchange;
    }

    public void setExchange(String exchange) {
        this.exchange = exchange;
    }

    public String getRoutingKey() {
        return routingKey;
    }

    public void setRoutingKey(String routingKey) {
        this.routingKey = routingKey;
    }
}
