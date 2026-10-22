package com.duoc.adminrabbit.web.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * La API administradora no puede crear colas con nombres vacios ni
 * configuraciones invalidas: se valida con las anotaciones del DTO y este
 * test lo comprueba sin levantar broker.
 */
class DtoValidacionTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    private NuevaColaRequest cola(String nombre) {
        NuevaColaRequest request = new NuevaColaRequest();
        request.setNombre(nombre);
        return request;
    }

    private Set<String> camposInvalidos(Object objeto) {
        return validator.validate(objeto).stream()
                .map(violacion -> violacion.getPropertyPath().toString())
                .collect(Collectors.toSet());
    }

    @Test
    void nombreDeColaVacio_tieneViolacion() {
        assertTrue(camposInvalidos(cola("")).contains("nombre"));
        assertTrue(camposInvalidos(cola("   ")).contains("nombre"));
    }

    @Test
    void nombreDeColaNulo_tieneViolacion() {
        assertTrue(camposInvalidos(cola(null)).contains("nombre"));
    }

    @Test
    void nombreConEspacios_tieneViolacion() {
        assertTrue(camposInvalidos(cola("cola mala")).contains("nombre"));
    }

    @Test
    void nombreConTildeOEnie_tieneViolacion() {
        assertTrue(camposInvalidos(cola("colañ")).contains("nombre"));
    }

    @Test
    void nombreValido_noTieneViolaciones() {
        assertTrue(validator.validate(cola("pedidos.pendientes_dlq-1")).isEmpty());
    }

    @Test
    void exchangeValido_noTieneViolaciones() {
        NuevaColaRequest request = cola("pedidos.pendientes");
        request.setExchange("pedidos.topic");
        request.setRoutingKey("pedido.creado");
        assertTrue(validator.validate(request).isEmpty());
    }

    @Test
    void exchangeConCaracteresInvalidos_tieneViolacion() {
        NuevaColaRequest request = cola("pedidos.pendientes");
        request.setExchange("exchange malo!");
        assertTrue(camposInvalidos(request).contains("exchange"));
    }

    @Test
    void nombreDeExchangeVacio_tieneViolacion() {
        NuevaExchangeRequest request = new NuevaExchangeRequest();
        request.setNombre("");
        assertTrue(camposInvalidos(request).contains("nombre"));
    }

    @Test
    void nombreDeExchangeValido_conOTipoSinTipo_noTieneViolaciones() {
        NuevaExchangeRequest conTipo = new NuevaExchangeRequest();
        conTipo.setNombre("pedidos.topic");
        conTipo.setTipo(TipoExchange.TOPIC);
        assertTrue(validator.validate(conTipo).isEmpty());

        NuevaExchangeRequest sinTipo = new NuevaExchangeRequest();
        sinTipo.setNombre("pedidos.fanout");
        assertTrue(validator.validate(sinTipo).isEmpty());
    }
}
