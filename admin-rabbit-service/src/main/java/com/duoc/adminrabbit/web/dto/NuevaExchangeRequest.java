package com.duoc.adminrabbit.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Pedido para crear un exchange. El tipo es opcional: por defecto DIRECT. */
public class NuevaExchangeRequest {

    @NotBlank(message = "el nombre del exchange es obligatorio")
    @Size(max = 255, message = "el nombre del exchange no puede superar 255 caracteres")
    @Pattern(
            regexp = "^[a-zA-Z0-9._-]+$",
            message = "el nombre solo puede contener letras, numeros, punto, guion bajo y guion (sin espacios)")
    private String nombre;

    /** DIRECT, TOPIC, FANOUT o HEADERS. Si no viene, se crea como DIRECT. */
    private TipoExchange tipo;

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public TipoExchange getTipo() {
        return tipo;
    }

    public void setTipo(TipoExchange tipo) {
        this.tipo = tipo;
    }
}
