package com.ventasdiscos.mscatalog.security;

import com.ventasdiscos.mscatalog.client.AccountsClient;
import com.ventasdiscos.mscatalog.exception.NoAutenticadoException;
import com.ventasdiscos.mscatalog.exception.ServicioExternoNoDisponibleException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.MethodParameter;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Cierre del backend · RN-44 y RF-CTA-08 CA-06 (decisión de cierre 1/4): un token todavía vigente NO autoriza nada si
 * la cuenta del CLIENTE dejó de estar ACTIVA (EN_ELIMINACION, ELIMINADA o estado no reconocido).
 * <ul>
 *   <li>Se aplica a toda petición cuyo controller recibe {@link AuthUser} (lecturas y escrituras: GET, POST, PUT,
 *       PATCH, DELETE). Los endpoints públicos no reciben AuthUser y no se revisan.</li>
 *   <li>El userId sale SOLO del JWT validado (nunca de la URL o del cuerpo).</li>
 *   <li>Consulta {@code GET /api/v1/internal/accounts/{userId}/operation-status} con el AccountsClient existente
 *       (JWT de servicio DOC-05 y X-Correlation-Id). ACTIVA → sigue; no ACTIVA o cuenta inexistente → 403;
 *       Accounts sin respuesta → 503. Un error de Accounts nunca se toma como ACTIVA.</li>
 *   <li>No se aplica: a ADMINISTRADOR (sus permisos no cambian; las cuentas de administrador no se eliminan), a las
 *       rutas internas con JWT de servicio, ni si falta el token o es inválido (eso lo responde el resolver con 401).</li>
 *   <li>Una misma petición consulta a Accounts una sola vez: AccountsClient recuerda en la petición la cuenta ya
 *       confirmada, así la comprobación previa a guardar (Lote 5) no repite la llamada.</li>
 * </ul>
 * Nunca se registra el token.
 */
public class CuentaActivaInterceptor implements HandlerInterceptor {

    // Constante ADMINISTRADOR: valor compartido que no cambia durante la ejecución.
    static final String ADMINISTRADOR = "ADMINISTRADOR";

    // Dependencia inyectada para acceder a jwt service sin crearla manualmente.
    private final ObjectProvider<JwtService> jwtService;
    // Dependencia inyectada para acceder a accounts client sin crearla manualmente.
    private final ObjectProvider<AccountsClient> accountsClient;

    // Ejecuta cuenta activa interceptor como parte de las responsabilidades de esta clase.
    public CuentaActivaInterceptor(ObjectProvider<JwtService> jwtService, ObjectProvider<AccountsClient> accountsClient) {
        this.jwtService = jwtService;
        this.accountsClient = accountsClient;
    }

    @Override
    // Ejecuta pre handle como parte de las responsabilidades de esta clase.
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod metodo) || !recibeUsuario(metodo)
                || request.getAttribute(ServicioInternoInterceptor.ATRIBUTO_EMISOR) != null
                || ServicioInternoInterceptor.esRutaInterna(request)) {
            return true;
        }
        String header = request.getHeader("Authorization");
        JwtService jwt = jwtService.getIfAvailable();
        if (header == null || !header.startsWith("Bearer ") || jwt == null) {
            return true; // el resolver de AuthUser responde 401
        }
        AuthUser usuario;
        try {
            usuario = jwt.validar(header.substring(7).trim());
        } catch (NoAutenticadoException e) {
            return true; // token inválido o vencido: 401 en el resolver
        }
        if (ADMINISTRADOR.equals(usuario.rol())) {
            return true;
        }
        AccountsClient cuentas = accountsClient.getIfAvailable();
        if (cuentas == null) {
            throw new ServicioExternoNoDisponibleException(NO_DISPONIBLE);
        }
        cuentas.exigirCuentaActiva(usuario.userId());
        return true;
    }

    // Constante NO_DISPONIBLE: valor compartido que no cambia durante la ejecución.
    static final String NO_DISPONIBLE = "No fue posible confirmar el estado de tu cuenta. Intenta nuevamente en unos minutos";

    // Comprobación de seguridad o identidad necesaria para permitir la operación. Método recibeUsuario (recibe usuario).
    private static boolean recibeUsuario(HandlerMethod metodo) {
        for (MethodParameter p : metodo.getMethodParameters()) {
            if (AuthUser.class.equals(p.getParameterType())) {
                return true;
            }
        }
        return false;
    }
}
