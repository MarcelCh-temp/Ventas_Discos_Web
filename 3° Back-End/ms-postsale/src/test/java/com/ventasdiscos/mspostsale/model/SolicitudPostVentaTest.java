package com.ventasdiscos.mspostsale.model;

import com.ventasdiscos.mspostsale.exception.OperacionNoPermitidaException;
import com.ventasdiscos.mspostsale.exception.RecursoNoEncontradoException;
import com.ventasdiscos.mspostsale.exception.ValidacionNegocioException;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Lote 4 · Reglas de la entidad única SolicitudPostVenta (UML 04, RN-41, DOC-09) sin base de datos. */
class SolicitudPostVentaTest {

    private static Map<Long, Integer> productos(long productoId, int cantidad) {
        Map<Long, Integer> m = new LinkedHashMap<>();
        m.put(productoId, cantidad);
        return m;
    }

    @Test
    void unaCancelacionNaceArmadaYPendiente() {
        SolicitudPostVenta s = SolicitudPostVenta.cancelacion(15L, 10L, "  Me equivoqué  ");
        assertEquals(TipoSolicitud.CANCELACION, s.getTipo());
        assertEquals(EstadoSolicitud.PENDIENTE, s.getEstado());
        assertEquals("Me equivoqué", s.getMotivoSolicitud());
        assertTrue(s.estaPendiente());
        assertTrue(s.perteneceA(10L));
        assertFalse(s.perteneceA(11L));
        assertTrue(s.estaMigrada());
        assertNull(s.getUsuarioResolucionId());
    }

    @Test
    void elMotivoEsObligatorioYTieneLargoMaximo() {
        assertThrows(ValidacionNegocioException.class, () -> SolicitudPostVenta.cancelacion(1L, 1L, " "));
        assertThrows(ValidacionNegocioException.class, () -> SolicitudPostVenta.cancelacion(1L, 1L, "x".repeat(256)));
    }

    @Test
    void aprobarGuardaMotivoFechaYResponsableUnaSolaVez() {
        SolicitudPostVenta s = SolicitudPostVenta.cancelacion(15L, 10L, "x");
        s.aprobar("Procede", 1L);
        assertEquals(EstadoSolicitud.APROBADA, s.getEstado());
        assertEquals("Procede", s.getMotivoResolucion());
        assertNotNull(s.getFechaResolucion());
        assertEquals(1L, s.getUsuarioResolucionId());
        // No se resuelve dos veces ni se sobrescribe el responsable (DOC-09)
        assertThrows(OperacionNoPermitidaException.class, () -> s.aprobar("Otra", 2L));
        assertThrows(OperacionNoPermitidaException.class, () -> s.rechazar("Otra", 2L));
        assertEquals(1L, s.getUsuarioResolucionId());
    }

    @Test
    void resolverExigeMotivoYResponsable() {
        SolicitudPostVenta s = SolicitudPostVenta.cancelacion(15L, 10L, "x");
        assertThrows(ValidacionNegocioException.class, () -> s.rechazar("", 1L));
        assertThrows(IllegalArgumentException.class, () -> s.rechazar("No procede", null));
        assertTrue(s.estaPendiente());
    }

    @Test
    void unaDevolucionExigeAlMenosUnProducto() {
        assertThrows(ValidacionNegocioException.class,
                () -> SolicitudPostVenta.devolucion(15L, 10L, "x", new LinkedHashMap<>()));
        SolicitudPostVenta s = SolicitudPostVenta.devolucion(15L, 10L, "Rayado", productos(2L, 2));
        assertEquals(TipoSolicitud.DEVOLUCION, s.getTipo());
        assertEquals(1, s.getItems().size());
        assertEquals(2, s.cantidadSolicitada(2L));
        assertTrue(s.incluyeProducto(2L));
        assertFalse(s.incluyeProducto(3L));
        assertThrows(IllegalArgumentException.class, () -> SolicitudPostVenta.devolucion(15L, 10L, "x", productos(2L, 0)));
    }

    @Test
    void laRecepcionSoloSeRegistraEnUnaDevolucionAprobadaYNoSuperaLoAutorizado() {
        SolicitudPostVenta s = SolicitudPostVenta.devolucion(15L, 10L, "Rayado", productos(2L, 2));
        assertThrows(OperacionNoPermitidaException.class, () -> s.registrarRecepcion("x", 1L));
        s.aprobar("Ok", 1L);
        RecepcionDevolucion r = s.registrarRecepcion("  Caja abierta ", 3L);
        assertEquals("Caja abierta", r.getObservaciones());
        assertEquals(3L, r.getUsuarioRecepcionId());
        ItemDevolucion item = s.getItems().get(0);
        r.agregarItem(item, 1, CondicionProducto.BUENA);
        assertEquals(1, s.cantidadRecibida(item));
        assertEquals(1, s.pendienteDeRecibir(item));
        assertThrows(IllegalArgumentException.class, () -> r.agregarItem(item, 2, CondicionProducto.BUENA));
        assertThrows(IllegalArgumentException.class, () -> s.registrarRecepcion("x", null));
    }

    @Test
    void soloLaUnidadBuenaEsAptaParaReponer() {
        SolicitudPostVenta s = SolicitudPostVenta.devolucion(15L, 10L, "Rayado", productos(2L, 2));
        s.aprobar("Ok", 1L);
        RecepcionDevolucion r = s.registrarRecepcion(null, 1L);
        ItemRecepcionDevolucion buena = r.agregarItem(s.getItems().get(0), 1, CondicionProducto.BUENA);
        ItemRecepcionDevolucion danada = r.agregarItem(s.getItems().get(0), 1, CondicionProducto.DANADA);
        assertTrue(buena.esAptaParaReposicion());
        assertFalse(danada.esAptaParaReposicion());
        assertNull(danada.prepararReposicion());
        // Sin id guardado no se puede armar la operacionId POSTSALE-RECEPCION-{id}
        assertThrows(IllegalStateException.class, buena::prepararReposicion);
    }

    @Test
    void elReembolsoNaceRegistradoSoloDesdeUnaSolicitudAprobadaPropia() {
        SolicitudPostVenta origen = SolicitudPostVenta.cancelacion(15L, 10L, "x");
        assertThrows(OperacionNoPermitidaException.class, () -> SolicitudPostVenta.reembolso(origen, 10L, null));
        origen.aprobar("Ok", 1L);
        assertThrows(RecursoNoEncontradoException.class, () -> SolicitudPostVenta.reembolso(origen, 11L, null));
        assertThrows(RecursoNoEncontradoException.class, () -> SolicitudPostVenta.reembolso(null, 10L, null));
        SolicitudPostVenta r = SolicitudPostVenta.reembolso(origen, 10L, " ");
        assertEquals(TipoSolicitud.REEMBOLSO, r.getTipo());
        assertEquals(EstadoSolicitud.REGISTRADA, r.getEstado());
        assertTrue(r.estaRegistrada());
        assertEquals(15L, r.getPedidoId());
        assertNull(r.getMotivoSolicitud());
        // RN-41: un reembolso no se aprueba ni se rechaza, y no origina otro reembolso
        assertThrows(OperacionNoPermitidaException.class, () -> r.aprobar("x", 1L));
        assertThrows(OperacionNoPermitidaException.class, () -> r.rechazar("x", 1L));
        assertThrows(ValidacionNegocioException.class, () -> SolicitudPostVenta.reembolso(r, 10L, null));
    }

    @Test
    void laOperacionDeReposicionSoloCambiaDesdePendiente() {
        OperacionReposicion op = new OperacionReposicion("POSTSALE-RECEPCION-1", 2);
        assertTrue(op.estaPendiente());
        op.marcarFallida();
        op.confirmar();
        assertEquals(EstadoReposicion.FALLIDA, op.getEstado());
        OperacionReposicion otra = new OperacionReposicion("POSTSALE-RECEPCION-2", 1);
        otra.confirmar();
        otra.marcarFallida();
        assertEquals(EstadoReposicion.CONFIRMADA, otra.getEstado());
    }
}
