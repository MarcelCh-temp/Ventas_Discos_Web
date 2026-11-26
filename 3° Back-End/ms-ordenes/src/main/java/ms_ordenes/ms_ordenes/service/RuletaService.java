package ms_ordenes.ms_ordenes.service;

import ms_ordenes.ms_ordenes.exception.OperacionNoPermitidaException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Map;

/** Un premio de ruleta por usuario, persistido en Orders. Nunca se confía en premios enviados por el navegador. */
@Service
public class RuletaService {
    private final JdbcTemplate jdbc;
    private final SecureRandom random = new SecureRandom();
    private final BigDecimal tarifaDespacho;

    public RuletaService(JdbcTemplate jdbc,
       @Value("${nackson.shipping.fee:3990}") BigDecimal tarifaDespacho) {
        this.jdbc = jdbc;
        this.tarifaDespacho = tarifaDespacho.max(BigDecimal.ZERO);
    }

    public record Estado(String premio, String estado, LocalDateTime proximoGiro,
                         boolean puedeGirar, Long pedidoId) {}
    public record Cotizacion(BigDecimal total, BigDecimal descuentoRuleta,
                             BigDecimal costoEnvio, String premio) {}

    private record Registro(LocalDateTime ultimoGiro, String premio, String estado, Long pedidoId) {}

    private Registro leer(Long userId, boolean bloquear) {
        String sql = "SELECT ultimo_giro, premio, estado, pedido_id FROM ruleta_usuario WHERE user_id=?" +
                (bloquear ? " FOR UPDATE" : "");
        return jdbc.query(sql, (rs, row) -> new Registro(
                rs.getTimestamp("ultimo_giro") == null ? null : rs.getTimestamp("ultimo_giro").toLocalDateTime(),
                rs.getString("premio"), rs.getString("estado"),
                (Long) rs.getObject("pedido_id")), userId).stream().findFirst().orElse(null);
    }

    @Transactional(readOnly = true)
    public Estado estado(Long userId) {
        return mostrar(leer(userId, false));
    }

    private Estado mostrar(Registro r) {
        if (r == null) return new Estado("NINGUNO", "SIN_GIRO", null, true, null);
        if (r.ultimoGiro() == null) return new Estado("NINGUNO", "SIN_GIRO", null, true, null);
        LocalDateTime siguiente = r.ultimoGiro().plusHours(24);
        boolean premioPendiente = "PENDIENTE".equals(r.estado()) || "RESERVADO".equals(r.estado());
        boolean puede = !premioPendiente && !LocalDateTime.now().isBefore(siguiente);
        return new Estado(r.premio(), r.estado(), siguiente, puede, r.pedidoId());
    }

    @Transactional
    public Estado girar(Long userId) {
        // INSERT IGNORE asegura que los giros simultáneos compitan por UNA fila persistida.
        jdbc.update("INSERT IGNORE INTO ruleta_usuario(user_id, premio, estado) VALUES (?, 'NINGUNO', 'SIN_GIRO')", userId);
        Registro r = leer(userId, true);
        Estado actual = mostrar(r);
        if (!actual.puedeGirar()) {
            throw new OperacionNoPermitidaException("Ya giraste o tienes un premio pendiente. Próximo giro: " + actual.proximoGiro());
        }
        int n = random.nextInt(100);
        String premio = n < 25 ? "DESCUENTO_5" : n < 50 ? "DESCUENTO_10" : n < 75 ? "ENVIO_GRATIS" : "NINGUNO";
        String estado = "NINGUNO".equals(premio) ? "SIN_PREMIO" : "PENDIENTE";
        jdbc.update("UPDATE ruleta_usuario SET ultimo_giro=CURRENT_TIMESTAMP, premio=?, estado=?, pedido_id=NULL WHERE user_id=?",
                premio, estado, userId);
        return estado(userId);
    }

    /**
     * Cotización definitiva dentro de la transacción de creación del pedido.
     * Bloquea el premio hasta que se complete su reserva.
     */
    public Cotizacion cotizarParaCompra(Long userId, BigDecimal totalProductos, String metodoEntrega) {
        if (!org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive()) {
            throw new IllegalStateException("La cotización de compra requiere una transacción activa");
        }
        jdbc.update("INSERT IGNORE INTO ruleta_usuario(user_id, premio, estado) VALUES (?, 'NINGUNO', 'SIN_GIRO')", userId);
        return cotizarInterno(leer(userId, true), totalProductos, metodoEntrega);
    }

    public Cotizacion cotizar(Long userId, BigDecimal totalProductos, String metodoEntrega) {
        return cotizarInterno(leer(userId, false), totalProductos, metodoEntrega);
    }

    private Cotizacion cotizarInterno(Registro r, BigDecimal totalProductos, String metodoEntrega) {
        BigDecimal costo = "DESPACHO_DOMICILIO".equals(metodoEntrega) ? tarifaDespacho : BigDecimal.ZERO;
        String premio = r != null && "PENDIENTE".equals(r.estado()) ? r.premio() : "NINGUNO";
        // El premio de envío gratis solo se reserva/consume en una compra con despacho.
        if ("ENVIO_GRATIS".equals(premio) && !"DESPACHO_DOMICILIO".equals(metodoEntrega)) {
            premio = "NINGUNO";
        }
        BigDecimal descuento = switch (premio) {
            case "DESCUENTO_5" -> totalProductos.multiply(new BigDecimal("0.05")).setScale(0, RoundingMode.HALF_UP);
            case "DESCUENTO_10" -> totalProductos.multiply(new BigDecimal("0.10")).setScale(0, RoundingMode.HALF_UP);
            default -> BigDecimal.ZERO;
        };
        if ("ENVIO_GRATIS".equals(premio)) costo = BigDecimal.ZERO;
        return new Cotizacion(totalProductos.subtract(descuento).add(costo), descuento, costo, premio);
    }

    /** Se llama DENTRO de la transacción que crea la orden y vacía carrito. */
    public Cotizacion reservar(Long userId, Long ordenId, BigDecimal totalProductos,
                               String metodoEntrega, BigDecimal totalEsperado) {
        Registro r = leer(userId, true);
        Cotizacion c = cotizarInterno(r, totalProductos, metodoEntrega);
        if (totalEsperado != null && c.total().compareTo(totalEsperado) != 0) {
            throw new OperacionNoPermitidaException("Cambió el total o tu premio; vuelve a validar el checkout",
                Map.of("totalEsperado", "Total actualizado: " + c.total()));
        }
        if (!"NINGUNO".equals(c.premio())) {
            int filas = jdbc.update("UPDATE ruleta_usuario SET estado='RESERVADO', pedido_id=? WHERE user_id=? AND estado='PENDIENTE'",
                    ordenId, userId);
            if (filas != 1) throw new OperacionNoPermitidaException("Tu premio cambió; vuelve a validar el checkout");
        }
        return c;
    }

    @Transactional
    public void consumir(Long orderId) {
        jdbc.update("UPDATE ruleta_usuario SET estado='CONSUMIDO' WHERE pedido_id=? AND estado='RESERVADO'", orderId);
    }

    @Transactional
    public void liberar(Long orderId) {
        jdbc.update("UPDATE ruleta_usuario SET estado='PENDIENTE', pedido_id=NULL WHERE pedido_id=? AND estado='RESERVADO'", orderId);
    }
}
