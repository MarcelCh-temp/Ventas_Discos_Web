package com.ventasdiscos.msaccounts.service;

import com.ventasdiscos.msaccounts.client.DependenciasCuentaClient;
import com.ventasdiscos.msaccounts.client.DependenciasCuentaClient.DependenciaNoConfirmadaException;
import com.ventasdiscos.msaccounts.dto.EliminacionCuentaResponse;
import com.ventasdiscos.msaccounts.exception.AccesoDenegadoException;
import com.ventasdiscos.msaccounts.exception.ApiException;
import com.ventasdiscos.msaccounts.exception.OperacionNoPermitidaException;
import com.ventasdiscos.msaccounts.exception.RecursoNoEncontradoException;
import com.ventasdiscos.msaccounts.exception.ServicioNoDisponibleException;
import com.ventasdiscos.msaccounts.exception.ValidacionNegocioException;
import com.ventasdiscos.msaccounts.model.Usuario;
import com.ventasdiscos.msaccounts.repository.UsuarioRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.LongPredicate;

/**
 * Lote 5 · HU-78 / RF-CTA-08 / RN-35 a RN-38, RN-43, RN-44 · ERS 13.7 (flujo de eliminación).
 * <ol>
 *   <li><b>Confirmar</b> (transacción corta): el Cliente confirma con su contraseña y la cuenta pasa a
 *       EN_ELIMINACION. Al confirmarse esta transacción, los demás MS ya ven la cuenta bloqueada (RN-37, RN-44).</li>
 *   <li><b>Procesar</b> (una transacción con la fila BLOQUEADA, para que dos DELETE o la tarea programada no
 *       choquen): verificar Orders, Support y PostSale → depurar favoritos (Catalog) y notificaciones
 *       (Notifications) → verificar otra vez (reduce la ventana de carrera con una creación simultánea) →
 *       anonimizar y ELIMINADA.</li>
 * </ol>
 * Reglas de seguridad del proceso:
 * <ul>
 *   <li>Primer intento con impedimentos o sin poder verificar: nada se depuró todavía, así que la cuenta vuelve a
 *       ACTIVA (rechazarEliminacion) y se informa el motivo (409 o 503).</li>
 *   <li>Reintento (la cuenta ya estaba EN_ELIMINACION; pudo haberse depurado algo): no se reactiva. Sigue
 *       EN_ELIMINACION, se registra el motivo y se vuelve a intentar más tarde.</li>
 *   <li>Si una depuración falla, la cuenta sigue EN_ELIMINACION (nunca se declara ELIMINADA a medias). Las
 *       depuraciones son idempotentes: repetirlas no duplica efectos.</li>
 *   <li>Accounts nunca entra a la base de otro MS. Los pedidos, tickets y solicitudes no se borran.</li>
 *   <li>La contraseña no se guarda, no se registra en logs y no se devuelve.</li>
 * </ul>
 */
@Slf4j
@Service
public class EliminacionCuentaService {

    static final int LOTE_REINTENTO = 50;

    private enum Inicio { NUEVA, CONTINUACION, YA_ELIMINADA }

    /** Resultado del proceso: respuesta (200/202) o error a lanzar DESPUÉS de confirmar la transacción. */
    record Resultado(EliminacionCuentaResponse respuesta, ApiException error) {
    }

    private record Verificacion(Map<String, String> bloqueos, Map<String, String> fallas) {
        boolean libre() {
            return bloqueos.isEmpty() && fallas.isEmpty();
        }
    }

    private final UsuarioRepository usuarios;
    private final PasswordEncoder passwordEncoder;
    private final DependenciasCuentaClient dependencias;
    private final TransactionTemplate transaccion;

    public EliminacionCuentaService(UsuarioRepository usuarios, PasswordEncoder passwordEncoder,
                                    DependenciasCuentaClient dependencias, PlatformTransactionManager transactionManager) {
        this.usuarios = usuarios;
        this.passwordEncoder = passwordEncoder;
        this.dependencias = dependencias;
        this.transaccion = new TransactionTemplate(transactionManager);
    }

    /**
     * DELETE /api/v1/accounts/me. userId sale del JWT. Con la cuenta ACTIVA exige la contraseña actual; con la cuenta
     * ya EN_ELIMINACION es la continuación de una eliminación ya autorizada y no la vuelve a pedir.
     */
    public EliminacionCuentaResponse eliminar(Long userId, String password) {
        Inicio inicio = transaccion.execute(st -> iniciar(userId, password));
        if (inicio == Inicio.YA_ELIMINADA) {
            return transaccion.execute(st -> EliminacionCuentaResponse.from(buscar(userId), "La cuenta ya fue eliminada"));
        }
        Resultado r = procesar(userId, inicio == Inicio.NUEVA);
        if (r.error() != null) {
            throw r.error();
        }
        return r.respuesta();
    }

    /**
     * ERS 13.7: retoma las cuentas que quedaron EN_ELIMINACION (depuración caída, token expirado, proceso
     * interrumpido). La llama EliminacionReintentoConfig. Devuelve cuántas cuentas revisó.
     */
    public int reintentarPendientes() {
        List<Long> ids = usuarios.idsEnEliminacion(PageRequest.of(0, LOTE_REINTENTO));
        for (Long id : ids) {
            try {
                Resultado r = procesar(id, false);
                if (r.error() != null) {
                    log.warn("Reintento de eliminación de la cuenta {}: sigue EN_ELIMINACION. Motivo: {} {}", id,
                            r.error().getMessage(), r.error().getErrores());
                } else {
                    log.info("Reintento de eliminación de la cuenta {}: {}", id, r.respuesta().estado());
                }
            } catch (RuntimeException e) {
                log.error("Reintento de eliminación de la cuenta {} interrumpido; se volverá a intentar", id, e);
            }
        }
        return ids.size();
    }

    // ===================== Paso 1: confirmar =====================

    private Inicio iniciar(Long userId, String password) {
        Usuario u = usuarios.bloquear(userId).orElseThrow(() -> new RecursoNoEncontradoException("La cuenta no existe"));
        if (u.estaEliminada()) {
            return Inicio.YA_ELIMINADA;
        }
        if (u.esAdministrador()) {
            throw new AccesoDenegadoException("Solo un Cliente puede eliminar su propia cuenta");
        }
        if (u.estaEnEliminacion()) {
            log.info("Eliminación de la cuenta {}: continuación de una solicitud ya confirmada", userId);
            return Inicio.CONTINUACION;
        }
        if (!u.puedeOperar()) {
            throw new AccesoDenegadoException("La cuenta no está habilitada para operar");
        }
        if (password == null || password.isBlank()) {
            throw new ValidacionNegocioException("Debes ingresar tu contraseña actual para confirmar la eliminación",
                    Map.of("password", "Ingresa tu contraseña actual"));
        }
        if (!passwordEncoder.matches(password, u.getPasswordHash())) {
            throw new ValidacionNegocioException("La contraseña es incorrecta", Map.of("password", "La contraseña es incorrecta"));
        }
        u.iniciarEliminacion();
        log.info("Eliminación de la cuenta {} confirmada por el Cliente: EN_ELIMINACION", userId);
        return Inicio.NUEVA;
    }

    // ===================== Paso 2: procesar (fila bloqueada) =====================

    Resultado procesar(Long userId, boolean primerIntento) {
        return transaccion.execute(st -> {
            Usuario u = buscarBloqueada(userId);
            if (u.estaEliminada()) {
                return new Resultado(EliminacionCuentaResponse.from(u, "La cuenta ya fue eliminada"), null);
            }
            if (!u.estaEnEliminacion()) {
                // Otra operación simultánea la rechazó (volvió a ACTIVA) mientras esperábamos el bloqueo.
                return new Resultado(null, new OperacionNoPermitidaException(
                        "La eliminación ya no está en curso (estado actual: " + u.getEstado() + ")"));
            }
            Verificacion v = verificar(userId);
            if (!v.libre()) {
                if (primerIntento) {
                    u.rechazarEliminacion();
                }
                log.warn("Eliminación de la cuenta {} detenida (primer intento: {}). Impedimentos: {}. Sin confirmar: {}",
                        userId, primerIntento, v.bloqueos(), v.fallas());
                return new Resultado(null, error(v, primerIntento));
            }
            Map<String, String> depuracion = depurar(userId);
            if (!depuracion.isEmpty()) {
                log.warn("Eliminación de la cuenta {}: depuración pendiente {}; sigue EN_ELIMINACION", userId, depuracion);
                return new Resultado(EliminacionCuentaResponse.from(u, "Tu cuenta quedó bloqueada. La eliminación"
                        + " terminará automáticamente cuando todos los servicios respondan"), null);
            }
            // RN-37: segunda verificación justo antes de terminar. Reduce (no elimina) la ventana en que otro MS
            // consultó ACTIVA antes del paso 1 y guardó su registro después de la primera verificación.
            Verificacion final_ = verificar(userId);
            if (!final_.libre()) {
                log.warn("Eliminación de la cuenta {}: la verificación final encontró {} / {}; sigue EN_ELIMINACION",
                        userId, final_.bloqueos(), final_.fallas());
                return new Resultado(null, error(final_, false));
            }
            u.completarEliminacion();
            log.info("Cuenta {} ELIMINADA: datos personales de Accounts anonimizados; favoritos y notificaciones"
                    + " depurados; pedidos, tickets y solicitudes conservados", userId);
            return new Resultado(EliminacionCuentaResponse.from(u, "Tu cuenta fue eliminada. Tus pedidos se conservan"
                    + " como historial comercial"), null);
        });
    }

    /** RF-CTA-08 / RN-36: las tres consultas deben responder; ninguna falla se interpreta como "sin pendientes". */
    private Verificacion verificar(Long userId) {
        Map<String, String> bloqueos = new LinkedHashMap<>();
        Map<String, String> fallas = new LinkedHashMap<>();
        consultar("pedidos", dependencias::tienePedidosActivos, userId,
                "Tienes pedidos activos (pendientes, confirmados, en preparación o despachados)", bloqueos, fallas);
        consultar("soporte", dependencias::tieneTicketsAbiertos, userId,
                "Tienes solicitudes de soporte abiertas o en proceso", bloqueos, fallas);
        consultar("postventa", dependencias::tieneSolicitudesPendientes, userId,
                "Tienes solicitudes de postventa pendientes", bloqueos, fallas);
        return new Verificacion(bloqueos, fallas);
    }

    private static void consultar(String campo, LongPredicate consulta, Long userId, String motivo,
                                  Map<String, String> bloqueos, Map<String, String> fallas) {
        try {
            if (consulta.test(userId)) {
                bloqueos.put(campo, motivo);
            }
        } catch (DependenciaNoConfirmadaException e) {
            fallas.put(campo, "No fue posible verificarlo (" + e.getServicio() + " no respondió)");
        }
    }

    /** RN-43: depuraciones idempotentes. Devuelve las que no se pudieron confirmar (vacío = todo depurado). */
    private Map<String, String> depurar(Long userId) {
        Map<String, String> fallas = new LinkedHashMap<>();
        try {
            int n = dependencias.depurarFavoritos(userId);
            log.info("Cuenta {}: favoritos depurados en Catalog ({} en esta llamada)", userId, n);
        } catch (DependenciaNoConfirmadaException e) {
            fallas.put("favoritos", e.getServicio() + " no confirmó la depuración");
        }
        try {
            int n = dependencias.depurarNotificaciones(userId);
            log.info("Cuenta {}: notificaciones depuradas en Notifications ({} en esta llamada)", userId, n);
        } catch (DependenciaNoConfirmadaException e) {
            fallas.put("notificaciones", e.getServicio() + " no confirmó la depuración");
        }
        return fallas;
    }

    private static ApiException error(Verificacion v, boolean primerIntento) {
        Map<String, String> errores = new LinkedHashMap<>(v.bloqueos());
        v.fallas().forEach(errores::putIfAbsent);
        String estado = primerIntento ? " Tu cuenta sigue activa y no se eliminó nada."
                : " Tu cuenta sigue bloqueada y la eliminación se volverá a intentar.";
        if (!v.bloqueos().isEmpty()) {
            return new OperacionNoPermitidaException("No es posible eliminar la cuenta: hay operaciones pendientes." + estado,
                    errores);
        }
        return new ServicioNoDisponibleException("No fue posible verificar que no tengas operaciones pendientes."
                + estado + " Intenta nuevamente más tarde", errores);
    }

    private Usuario buscar(Long userId) {
        return usuarios.findById(userId).orElseThrow(() -> new RecursoNoEncontradoException("La cuenta no existe"));
    }

    private Usuario buscarBloqueada(Long userId) {
        return usuarios.bloquear(userId).orElseThrow(() -> new RecursoNoEncontradoException("La cuenta no existe"));
    }
}
