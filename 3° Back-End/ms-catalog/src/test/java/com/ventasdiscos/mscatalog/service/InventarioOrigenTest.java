package com.ventasdiscos.mscatalog.service;

import com.ventasdiscos.mscatalog.exception.AccesoDenegadoException;
import com.ventasdiscos.mscatalog.security.Servicios;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Lote 6 · DOC-08: ORDERS- solo desde ms-orders y POSTSALE- solo desde ms-postsale (regla en el Service, CSR). */
class InventarioOrigenTest {

    @Test
    void elPrefijoCorrespondeAlServicioAutenticado() {
        assertDoesNotThrow(() -> InventarioService.validarOrigen(Servicios.ORDERS, "ORDERS-PEDIDO-1-VENTA"));
        assertDoesNotThrow(() -> InventarioService.validarOrigen(Servicios.POSTSALE, "POSTSALE-RECEPCION-1"));
    }

    @Test
    void prefijoDeOtroServicioOSinEmisor_403() {
        assertThrows(AccesoDenegadoException.class,
                () -> InventarioService.validarOrigen(Servicios.POSTSALE, "ORDERS-PEDIDO-1-VENTA"));
        assertThrows(AccesoDenegadoException.class,
                () -> InventarioService.validarOrigen(Servicios.ORDERS, "POSTSALE-RECEPCION-1"));
        assertThrows(AccesoDenegadoException.class,
                () -> InventarioService.validarOrigen(Servicios.ACCOUNTS, "ORDERS-PEDIDO-1-VENTA"));
        assertThrows(AccesoDenegadoException.class,
                () -> InventarioService.validarOrigen(null, "ORDERS-PEDIDO-1-VENTA"));
    }
}
