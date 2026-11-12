package ms_ordenes.ms_ordenes.client;

import java.math.BigDecimal;
import java.util.List;

/**
 * Lote 3: copias de SOLO LECTURA de las respuestas de MS-Catalog (contratos en CONTRATOS_API_LOTE_2.md y
 * PricingResponse de ms-catalog). Orders no guarda estas copias: las usa para calcular y validar.
 */
public final class CatalogoDtos {

    private CatalogoDtos() {
    }

    public record ItemSolicitado(Long productId, int cantidad) {
    }

    /** POST /api/v1/internal/catalog/pricing (precio comercial + promociones vigentes). */
    public record Cotizacion(List<LineaCotizada> lineas, BigDecimal subtotal, BigDecimal descuentoTotal,
                             BigDecimal total, boolean todosComprables) {
    }

    public record LineaCotizada(Long productId, String nombre, int cantidad, BigDecimal precioUnitario,
                                BigDecimal descuentoUnitario, BigDecimal precioFinalUnitario, BigDecimal subtotal,
                                Long promocionId, boolean comprable, String motivo) {
    }

    /** POST /api/v1/inventory/availability/query (no expone el stock exacto). */
    public record Disponibilidad(Long productId, Integer cantidadSolicitada, String disponibilidad,
                                 boolean haySuficiente, boolean permiteCompra) {
    }

    /**
     * Resultado de POST /api/v1/internal/inventory/operations visto desde Orders.
     * Revisión Lote 3: un 4xx ya NO se interpreta siempre como "sin stock". Catalog responde 409 tanto por stock
     * insuficiente como por una operacionId ya registrada con otro contenido, así que el cliente solo informa
     * CONFLICTO; OrdenService decide SIN_STOCK únicamente si la consulta de disponibilidad lo confirma.
     */
    public enum ResultadoOperacion {
        /** 201 aplicada ahora o 200 ya aplicada antes (idempotencia por operacionId). */
        APLICADA,
        /** Lo asigna OrdenService (nunca el cliente): 409 + Catalog confirma que falta stock. No se movió stock. */
        SIN_STOCK,
        /** 409 sin falta de stock demostrada (por ejemplo, operacionId ya usada con otro contenido). Requiere revisión. */
        CONFLICTO,
        /** Otro 4xx (400 datos inválidos, 404 producto inexistente, 401/403): Catalog no aceptó la solicitud. */
        RECHAZADA,
        /** Catalog no respondió (conexión, tiempo de espera, 5xx): no se sabe si se aplicó. Reintentar es seguro. */
        NO_CONFIRMADA
    }

    /** status = código HTTP de Catalog (0 si no hubo respuesta). */
    public record RespuestaOperacion(ResultadoOperacion resultado, String mensaje, int status) {

        public RespuestaOperacion(ResultadoOperacion resultado, String mensaje) {
            this(resultado, mensaje, 0);
        }
    }
}
