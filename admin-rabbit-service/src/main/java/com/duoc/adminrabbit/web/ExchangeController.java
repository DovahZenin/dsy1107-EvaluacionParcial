package com.duoc.adminrabbit.web;

import com.duoc.adminrabbit.service.RabbitAdminService;
import com.duoc.adminrabbit.web.dto.ExchangeView;
import com.duoc.adminrabbit.web.dto.NuevaExchangeRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;

/**
 * API administradora de exchanges:
 *   GET    /exchanges            lista exchanges del sistema y los dinamicos
 *   POST   /exchanges            crea un exchange (409 si existe)
 *   DELETE /exchanges/{nombre}   borra un exchange dinamico (404/409)
 */
@RestController
@RequestMapping("/exchanges")
public class ExchangeController {

    private final RabbitAdminService servicio;

    public ExchangeController(RabbitAdminService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    public List<ExchangeView> listar() {
        return servicio.listarExchanges();
    }

    @PostMapping
    public ResponseEntity<ExchangeView> crear(
            @Valid @RequestBody NuevaExchangeRequest request,
            UriComponentsBuilder uriBuilder) {
        ExchangeView vista = servicio.crearExchange(request);
        URI ubicacion = uriBuilder.path("/exchanges/{nombre}").buildAndExpand(vista.nombre()).toUri();
        return ResponseEntity.created(ubicacion).body(vista);
    }

    @DeleteMapping("/{nombre}")
    public ResponseEntity<Void> eliminar(@PathVariable String nombre) {
        servicio.eliminarExchange(nombre);
        return ResponseEntity.noContent().build();
    }
}
