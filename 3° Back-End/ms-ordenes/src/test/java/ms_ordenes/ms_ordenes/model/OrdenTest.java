package ms_ordenes.ms_ordenes.model;

import ms_ordenes.ms_ordenes.exception.OperacionNoPermitidaException;
import ms_ordenes.ms_ordenes.exception.ValidacionNegocioException;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Lote 3: Orden (≡ Pedido del UML 02). Reemplaza al OrdenTest original, que probaba setters que ya no existen
 * (el estado y el total solo cambian con los métodos del modelo).
 */
class OrdenTest {

    static DatosEntrega despacho() {
        return DatosEntrega.crear(MetodoEntrega.DESPACHO_DOMICILIO, "Ana Pérez", "+56912345678", "Av. Siempre Viva 123",
                "Santiago", null);
    }

    static Orden orden(EstadoOrden... recorrido) {
        Orden o = Orden.crear(10L, List.of(
                        new ItemPedido(1L, "Disco", new BigDecimal("9990"), 2),
                        new ItemPedido(2L, "Juego", new BigDecimal("20000"), 1)),
                new BigDecimal("1000"), despacho(), "Pago simulado aprobado");
        ReflectionTestUtils.setField(o, "id", 15L);
        for (EstadoOrden e : recorrido) {
            o.cambiarEstado(e, e == EstadoOrden.CANCELADO ? "Motivo" : null, null, 1L);
        }
        return o;
    }

    @Test
    void crearCalculaElTotalEnElBackendYRegistraElHistorial() {
        Orden o = orden();
        assertEquals(new BigDecimal("39980"), o.getTotal());
        assertEquals(new BigDecimal("1000"), o.getDescuentoHistorico());
        assertEquals(EstadoOrden.PENDIENTE, o.getEstado());
        assertEquals(1, o.getHistorial().size());
        assertNull(o.getHistorial().get(0).getEstadoAnterior());
        assertEquals(10L, o.getHistorial().get(0).getUsuarioResponsableId());
        assertNotNull(o.getFechaCreacion());
    }

    @Test
    void pedidoSinProductosNoSeCrea() {
        assertThrows(ValidacionNegocioException.class, () -> Orden.crear(10L, List.of(), BigDecimal.ZERO, despacho(), "x"));
    }

    @Test
    void numeroDePedidoSeAsignaUnaVezDespuesDeGuardar() {
        Orden o = orden();
        o.asignarNumeroPedido();
        assertEquals("PED-000015", o.getNumeroPedido());
        ReflectionTestUtils.setField(o, "id", 99L);
        o.asignarNumeroPedido();
        assertEquals("PED-000015", o.getNumeroPedido());
    }

    @Test
    void flujoCompletoRegistraCadaCambio() {
        Orden o = orden(EstadoOrden.CONFIRMADO, EstadoOrden.PREPARANDO, EstadoOrden.DESPACHADO, EstadoOrden.ENTREGADO);
        assertEquals(EstadoOrden.ENTREGADO, o.getEstado());
        assertEquals(5, o.getHistorial().size());
        assertTrue(o.estaFinalizado());
        assertFalse(o.estaActivo());
    }

    /** RF-PED-04: Entregado -> Preparando y Cancelado -> Despachado se bloquean; el pedido no cambia. */
    @Test
    void transicionesIncompatiblesSeBloqueanSinCambiarNada() {
        Orden entregado = orden(EstadoOrden.CONFIRMADO, EstadoOrden.PREPARANDO, EstadoOrden.DESPACHADO, EstadoOrden.ENTREGADO);
        assertThrows(OperacionNoPermitidaException.class,
                () -> entregado.cambiarEstado(EstadoOrden.PREPARANDO, null, null, 1L));
        assertEquals(5, entregado.getHistorial().size());

        Orden cancelado = orden(EstadoOrden.CANCELADO);
        assertThrows(OperacionNoPermitidaException.class,
                () -> cancelado.cambiarEstado(EstadoOrden.DESPACHADO, null, null, 1L));
        assertEquals(EstadoOrden.CANCELADO, cancelado.getEstado());

        Orden pendiente = orden();
        assertThrows(OperacionNoPermitidaException.class,
                () -> pendiente.cambiarEstado(EstadoOrden.PREPARANDO, null, null, 1L)); // no se salta pasos
    }

    @Test
    void cancelarExigeMotivoYSoloAntesDelDespacho() {
        Orden o = orden(EstadoOrden.CONFIRMADO);
        assertThrows(ValidacionNegocioException.class, () -> o.cambiarEstado(EstadoOrden.CANCELADO, " ", null, 1L));
        assertTrue(o.puedeCancelarse());
        assertFalse(orden(EstadoOrden.CONFIRMADO, EstadoOrden.PREPARANDO, EstadoOrden.DESPACHADO).puedeCancelarse());
        assertFalse(orden(EstadoOrden.CANCELADO).puedeCancelarse());
    }

    @Test
    void corregirEntregaAntesDeDespachadoQuedaEnElHistorial() {
        Orden o = orden(EstadoOrden.CONFIRMADO);
        List<String> cambios = o.corregirDatosEntrega(null, "+56987654321", null, "Providencia", null, 1L);
        assertEquals(List.of("telefono", "comuna"), cambios);
        assertEquals("Providencia", o.getDatosEntrega().getComuna());
        assertEquals("Corrección de datos de entrega", o.getHistorial().get(o.getHistorial().size() - 1).getMotivo());
    }

    @Test
    void corregirEntregaDespuesDelDespachoSeRechaza() {
        Orden o = orden(EstadoOrden.CONFIRMADO, EstadoOrden.PREPARANDO, EstadoOrden.DESPACHADO);
        assertThrows(OperacionNoPermitidaException.class,
                () -> o.corregirDatosEntrega("Otro", null, null, null, null, 1L));
        assertEquals("Ana Pérez", o.getDatosEntrega().getNombreReceptor());
    }

    @Test
    void despachoEnPreparandoPasaADespachadoYLuegoSeActualiza() {
        Orden o = orden(EstadoOrden.CONFIRMADO, EstadoOrden.PREPARANDO);
        o.registrarDespacho("Chilexpress", null, null, 1L);
        assertEquals(EstadoOrden.DESPACHADO, o.getEstado());
        assertNull(o.getDespacho().getNumeroSeguimiento()); // sin tracking externo obligatorio
        o.registrarDespacho("Chilexpress", "CX-123", null, 1L);
        assertEquals("CX-123", o.getDespacho().getNumeroSeguimiento());
        assertEquals(EstadoOrden.DESPACHADO, o.getEstado());
    }

    @Test
    void despachoFueraDePreparandoODespachadoSeRechaza() {
        Orden o = orden(EstadoOrden.CONFIRMADO);
        assertThrows(OperacionNoPermitidaException.class, () -> o.registrarDespacho("Chilexpress", null, null, 1L));
        assertNull(o.getDespacho());
        Orden p = orden(EstadoOrden.CONFIRMADO, EstadoOrden.PREPARANDO);
        assertThrows(ValidacionNegocioException.class, () -> p.registrarDespacho(" ", null, null, 1L));
        assertEquals(EstadoOrden.PREPARANDO, p.getEstado());
    }

    @Test
    void listasDeSoloLecturaYSnapshotDePrecios() {
        Orden o = orden();
        assertThrows(UnsupportedOperationException.class, () -> o.getItems().clear());
        assertThrows(UnsupportedOperationException.class, () -> o.getHistorial().clear());
        assertEquals(new BigDecimal("19980"), o.getItems().get(0).getSubtotalHistorico());
        assertEquals(new BigDecimal("19980"), o.getItems().get(0).calcularSubtotal());
    }

    @Test
    void perteneceSoloASuDueno() {
        assertTrue(orden().perteneceA(10L));
        assertFalse(orden().perteneceA(11L));
    }
}
