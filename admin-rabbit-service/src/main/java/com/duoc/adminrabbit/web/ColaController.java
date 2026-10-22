package com.duoc.adminrabbit.web;

import com.duoc.adminrabbit.service.RabbitAdminService;
import com.duoc.adminrabbit.web.dto.ColaView;
import com.duoc.adminrabbit.web.dto.NuevaColaRequest;
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
 * API administradora de colas:
 *   GET    /queues            lista colas del sistema y las dinamicas
 *   GET    /queues/{nombre}   detalle de una cola (404 si no existe)
 *   POST   /queues            crea cola + DLQ + bindings (409 si existe)
 *   DELETE /queues/{nombre}   borra una cola dinamica (404/409)
 */
@RestController
@RequestMapping("/queues")
public class ColaController {

    private final RabbitAdminService servicio;

    public ColaController(RabbitAdminService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    public List<ColaView> listar() {
        return servicio.listarColas();
    }

    @GetMapping("/{nombre}")
    public ColaView obtener(@PathVariable String nombre) {
        return servicio.obtenerCola(nombre);
    }

    @PostMapping
    public ResponseEntity<ColaView> crear(
            @Valid @RequestBody NuevaColaRequest request,
            UriComponentsBuilder uriBuilder) {
        ColaView vista = servicio.crearCola(request);
        URI ubicacion = uriBuilder.path("/queues/{nombre}").buildAndExpand(vista.nombre()).toUri();
        return ResponseEntity.created(ubicacion).body(vista);
    }

    @DeleteMapping("/{nombre}")
    public ResponseEntity<Void> eliminar(@PathVariable String nombre) {
        servicio.eliminarCola(nombre);
        return ResponseEntity.noContent().build();
    }
}
