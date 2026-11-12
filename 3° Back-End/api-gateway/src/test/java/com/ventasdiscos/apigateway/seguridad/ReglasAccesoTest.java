package com.ventasdiscos.apigateway.seguridad;

import com.ventasdiscos.apigateway.seguridad.ReglasAcceso.Acceso;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Lote 6 · Decisión 3/4: clasificación de rutas según los contratos de los Lotes 1 a 5. */
class ReglasAccesoTest {

    @ParameterizedTest(name = "{0} {1} es pública")
    @CsvSource({
            "POST, /api/v1/accounts/register",
            "POST, /api/v1/auth/login",
            "POST, /api/v1/auth/recovery/password",
            "POST, /api/v1/auth/recovery/email",
            "GET, /api/v1/products",
            "GET, /api/v1/products/15",
            "HEAD, /api/v1/products/15",
            "GET, /api/v1/offers",
            "GET, /api/v1/inventory/15/availability",
            "POST, /api/v1/inventory/availability/query",
            "GET, /api/v1/checkout/delivery-methods",
            "GET, /api/v1/checkout/payment-methods"})
    void rutasPublicasDelContrato(String metodo, String path) {
        assertEquals(Acceso.PUBLICA, ReglasAcceso.clasificar(metodo, path));
    }

    @ParameterizedTest(name = "{0} {1} exige sesión")
    @CsvSource({
            // mismo path, otro método: deja de ser público
            "GET, /api/v1/auth/login",
            "PUT, /api/v1/products/15",
            "DELETE, /api/v1/products/15",
            "POST, /api/v1/checkout/validate",
            "POST, /api/v1/checkout/confirm",
            "POST, /api/v1/auth/logout",
            "GET, /api/v1/accounts/me",
            "DELETE, /api/v1/accounts/me",
            "GET, /api/v1/favorites",
            "GET, /api/v1/cart",
            "GET, /api/v1/orders/me",
            "GET, /api/v1/orders/me/AK-1/tracking",
            "POST, /api/v1/post-sale/cancellations",
            "POST, /api/v1/post-sale/refund-requests",
            "GET, /api/v1/notifications/me",
            "PATCH, /api/v1/notifications/read-all",
            "POST, /api/v1/support/requests",
            // barra final: no coincide con la regla pública exacta, queda protegida
            "GET, /api/v1/products/"})
    void rutasDeCliente(String metodo, String path) {
        assertEquals(Acceso.CLIENTE, ReglasAcceso.clasificar(metodo, path));
    }

    @ParameterizedTest(name = "{0} {1} es administrativa")
    @CsvSource({
            "GET, /api/v1/admin/products",
            "POST, /api/v1/admin/products/retire",
            "GET, /api/v1/admin/inventory/movements",
            "PATCH, /api/v1/admin/promotions/3/deactivate",
            "GET, /api/v1/admin/orders",
            "POST, /api/v1/admin/post-sale/returns/4/reception",
            "POST, /api/v1/admin/support/requests/9/close",
            // administrativa SIN el prefijo /admin: no debe escaparse como ruta de cliente
            "GET, /api/v1/accounts/stats",
            // Cierre (decisión 3/4): consulta agrupada de clientes, solo administrador
            "GET, /api/v1/admin/accounts",
            "GET, /api/v1/accounts/STATS",
            "GET, /api/v1/accounts/stats/"})
    void rutasAdministrativas(String metodo, String path) {
        assertEquals(Acceso.ADMINISTRADOR, ReglasAcceso.clasificar(metodo, path));
    }

    @ParameterizedTest(name = "{0} nunca se expone")
    @ValueSource(strings = {
            "/api/v1/internal/accounts/5/operation-status",
            "/api/v1/internal/catalog/pricing",
            "/api/v1/internal/inventory/operations",
            "/api/v1/internal/orders/7",
            "/api/v1/internal/post-sale/users/5/pending-exists",
            "/api/v1/internal/notifications/events",
            "/api/v1/internal/support/users/5/open-exists",
            "/api/v1/INTERNAL/orders/7",
            "/API/V1/internal/orders/7",
            "/api/v1/internal"})
    void rutasInternas(String path) {
        assertEquals(Acceso.INTERNA, ReglasAcceso.clasificar("POST", path));
    }

    @ParameterizedTest(name = "{0} no está enrutada")
    @ValueSource(strings = {
            "/", "/api/v1", "/api/v1/admin", "/api/v1/admin/otra-cosa", "/api/v1/productsX", "/api/ordenes",
            "/api/soporte/tickets", "/actuator/health", "/swagger-ui/index.html", "/api/v1/ADMIN/orders",
            "/api/v1/inventario"})
    void rutasNoExpuestas(String path) {
        assertEquals(Acceso.NO_EXPUESTA, ReglasAcceso.clasificar("GET", path));
    }

    @ParameterizedTest(name = "{0} es sospechosa")
    @ValueSource(strings = {
            "/api/v1/%61dmin/orders", "/api/v1/orders/%2e%2e/admin/orders", "/api/v1/orders/..%2fadmin",
            "/api/v1/accounts/%73tats", "/api/v1/orders/../admin/orders", "/api/v1/./admin/orders",
            "/api/v1//admin/orders", "/api/v1/orders;x=1/me", "/api/v1/orders\\me", "", "api/v1/products"})
    void rutasSospechosas(String uri) {
        assertTrue(ReglasAcceso.rutaSospechosa(uri));
    }

    @ParameterizedTest(name = "{0} es normal")
    @ValueSource(strings = {"/api/v1/products", "/api/v1/products/15", "/api/v1/orders/me/AK-2026-1/tracking",
            "/api/v1/admin/inventory/movements"})
    void rutasNormales(String uri) {
        assertFalse(ReglasAcceso.rutaSospechosa(uri));
    }
}
