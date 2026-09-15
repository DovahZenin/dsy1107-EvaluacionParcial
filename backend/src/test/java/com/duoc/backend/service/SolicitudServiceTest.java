package com.duoc.backend.service;

import com.duoc.backend.model.Solicitud;
import com.duoc.backend.repository.SolicitudRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SolicitudServiceTest {

    @Mock
    private SolicitudRepository repository;

    @InjectMocks
    private SolicitudService service;

    @Test
    void crearSolicitud_asignaUsuarioYFuerzaEstadoPendiente() {
        Solicitud solicitud = new Solicitud();
        solicitud.setEstado(Solicitud.EstadoSolicitud.APROBADA);

        service.crearSolicitud(solicitud, "usuario-1");

        assertEquals("usuario-1", solicitud.getUsuarioId());
        assertEquals(Solicitud.EstadoSolicitud.PENDIENTE, solicitud.getEstado());
        verify(repository).save(solicitud);
    }

    @Test
    void crearSolicitud_devuelveLoQueGuardaElRepositorio() {
        Solicitud solicitud = new Solicitud();
        Solicitud guardada = new Solicitud();
        guardada.setId(42L);
        when(repository.save(solicitud)).thenReturn(guardada);

        Solicitud resultado = service.crearSolicitud(solicitud, "usuario-1");

        assertSame(guardada, resultado);
    }

    @Test
    void obtenerPorUsuario_devuelveSolicitudesDelRepositorio() {
        List<Solicitud> esperada = List.of(new Solicitud(), new Solicitud());
        when(repository.findByUsuarioId("usuario-1")).thenReturn(esperada);

        List<Solicitud> resultado = service.obtenerPorUsuario("usuario-1");

        assertSame(esperada, resultado);
    }

    @Test
    void obtenerPorUsuario_sinRegistros_devuelveListaVacia() {
        when(repository.findByUsuarioId("usuario-1")).thenReturn(List.of());

        List<Solicitud> resultado = service.obtenerPorUsuario("usuario-1");

        assertTrue(resultado.isEmpty());
    }

    @Test
    void obtenerTodas_devuelveTodasLasSolicitudes() {
        List<Solicitud> esperada = List.of(new Solicitud(), new Solicitud(), new Solicitud());
        when(repository.findAll()).thenReturn(esperada);

        List<Solicitud> resultado = service.obtenerTodas();

        assertSame(esperada, resultado);
    }

    @Test
    void eliminarSolicitud_llamaDeletePorId() {
        service.eliminarSolicitud(10L);

        verify(repository).deleteById(10L);
    }

    @Test
    void evaluarSolicitud_estadoEnMinusculaSeNormalizaAEnum() {
        Solicitud solicitud = new Solicitud();
        solicitud.setUsuarioId("usuario-1");
        when(repository.findById(5L)).thenReturn(Optional.of(solicitud));
        when(repository.save(solicitud)).thenReturn(solicitud);

        Solicitud resultado = service.evaluarSolicitud(5L, "aprobada", "Todo ok");

        assertEquals(Solicitud.EstadoSolicitud.APROBADA, resultado.getEstado());
        assertEquals("Todo ok", resultado.getComentarioAprobador());
        verify(repository).save(solicitud);
    }

    @Test
    void evaluarSolicitud_idInexistente_lanzaRuntimeException() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.evaluarSolicitud(99L, "APROBADA", "ok"));

        assertEquals("Solicitud no encontrada", ex.getMessage());
    }

    @Test
    void evaluarSolicitud_estadoInvalido_lanzaIllegalArgumentException() {
        Solicitud solicitud = new Solicitud();
        when(repository.findById(5L)).thenReturn(Optional.of(solicitud));

        assertThrows(IllegalArgumentException.class,
                () -> service.evaluarSolicitud(5L, "XYZ", "ok"));
    }
}