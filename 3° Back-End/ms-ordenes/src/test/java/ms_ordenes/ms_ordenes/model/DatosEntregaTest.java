package ms_ordenes.ms_ordenes.model;

import ms_ordenes.ms_ordenes.exception.ValidacionNegocioException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Lote 3 (RF-COM-05/06): los obligatorios dependen del método; retiro no exige dirección. */
class DatosEntregaTest {

    @Test
    void retiroExigeNombreYTelefonoYNoGuardaDireccion() {
        DatosEntrega d = DatosEntrega.crear(MetodoEntrega.RETIRO_TIENDA, "Ana", "912345678", "Calle 1", "Santiago", "");
        assertNull(d.getDireccion());
        assertNull(d.getComuna());
        assertNull(d.getReferencia());
        ValidacionNegocioException e = assertThrows(ValidacionNegocioException.class,
                () -> DatosEntrega.crear(MetodoEntrega.RETIRO_TIENDA, "Ana", "  ", null, null, null));
        assertTrue(e.getErrores().containsKey("telefono"));
    }

    @Test
    void despachoExigeDireccionYComuna() {
        ValidacionNegocioException e = assertThrows(ValidacionNegocioException.class,
                () -> DatosEntrega.crear(MetodoEntrega.DESPACHO_DOMICILIO, "Ana", "912345678", null, "", "Casa azul"));
        assertTrue(e.getErrores().containsKey("direccion"));
        assertTrue(e.getErrores().containsKey("comuna"));
        assertEquals(2, e.getErrores().size());
    }

    @Test
    void sinMetodoSeRechaza() {
        ValidacionNegocioException e = assertThrows(ValidacionNegocioException.class,
                () -> DatosEntrega.crear(null, "Ana", "912345678", null, null, null));
        assertTrue(e.getErrores().containsKey("metodoEntrega"));
    }

    @Test
    void corregirNoDejaVacioUnObligatorioNiCambiaNada() {
        DatosEntrega d = OrdenTest.despacho();
        assertThrows(ValidacionNegocioException.class, () -> d.corregir(null, null, "", null, null));
        assertEquals("Av. Siempre Viva 123", d.getDireccion());
    }

    @Test
    void enRetiroNoSeAceptaDireccionAlCorregir() {
        DatosEntrega d = DatosEntrega.crear(MetodoEntrega.RETIRO_TIENDA, "Ana", "912345678", null, null, null);
        ValidacionNegocioException e = assertThrows(ValidacionNegocioException.class,
                () -> d.corregir(null, null, "Calle 2", null, null));
        assertTrue(e.getErrores().containsKey("direccion"));
    }

    @Test
    void metodosPublicanSusCamposObligatorios() {
        assertEquals(2, MetodoEntrega.RETIRO_TIENDA.camposObligatorios().size());
        assertTrue(MetodoEntrega.DESPACHO_DOMICILIO.camposObligatorios().contains("comuna"));
    }
}
