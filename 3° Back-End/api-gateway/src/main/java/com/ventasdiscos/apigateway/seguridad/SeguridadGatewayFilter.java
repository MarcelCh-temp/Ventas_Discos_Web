package com.ventasdiscos.apigateway.seguridad;

import com.ventasdiscos.apigateway.error.ApiErrorJson;
import com.ventasdiscos.apigateway.seguridad.ReglasAcceso.Acceso;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Lote 6 · Decisión 3/4 (ERS 12.6): primera capa de control del Gateway, ANTES de reenviar al microservicio.
 * <ul>
 *   <li>Ruta escrita de forma sospechosa → 400.</li>
 *   <li>/api/v1/internal/** o ruta que el Gateway no enruta → 404 (nunca llega a un MS).</li>
 *   <li>Ruta pública → pasa sin token (si trae uno, no se revisa: un token viejo no bloquea el login).</li>
 *   <li>Ruta de cliente o administrador sin token, con token inválido o expirado → 401.</li>
 *   <li>Ruta administrativa con un usuario que no es ADMINISTRADOR → 403.</li>
 * </ul>
 * El token se reenvía tal cual: cada MS vuelve a validarlo y aplica rol, propiedad del recurso y estado de la cuenta
 * (un token vigente no permite operar si la cuenta está EN_ELIMINACION o ELIMINADA, Lote 5).
 * Corre después de CORS: las solicitudes preflight OPTIONS las responde el filtro CORS y no llegan aquí.
 * Nunca se registra el token en los logs.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 30)
public class SeguridadGatewayFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(SeguridadGatewayFilter.class);
    // Constante RUTA_INVALIDA: valor compartido que no cambia durante la ejecución.
    static final String RUTA_INVALIDA = "La ruta solicitada no es válida";
    // Constante NO_ENCONTRADO: valor compartido que no cambia durante la ejecución.
    static final String NO_ENCONTRADO = "El recurso solicitado no existe";
    // Constante SIN_SESION: valor compartido que no cambia durante la ejecución.
    static final String SIN_SESION = "Debes iniciar sesión para acceder a este recurso";
    // Constante SIN_PERMISO: valor compartido que no cambia durante la ejecución.
    static final String SIN_PERMISO = "No tienes permisos para acceder a este recurso";

    // Atributo validator: dato o dependencia utilizada por esta clase.
    private final JwtUsuarioValidator validator;

    // Ejecuta seguridad gateway filter como parte de las responsabilidades de esta clase.
    public SeguridadGatewayFilter(JwtUsuarioValidator validator) {
        this.validator = validator;
    }

    @Override
    // Operación auxiliar que forma parte de la responsabilidad de esta clase. Método doFilterInternal (do filter internal).
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String uri = request.getRequestURI();
        if (ReglasAcceso.rutaSospechosa(uri)) {
            log.warn("Ruta rechazada por formato sospechoso: {} {}", request.getMethod(), uri);
            ApiErrorJson.escribir(request, response, HttpStatus.BAD_REQUEST, RUTA_INVALIDA);
            return;
        }
        String contexto = request.getContextPath();
        String path = contexto != null && !contexto.isEmpty() && uri.startsWith(contexto) ? uri.substring(contexto.length()) : uri;
        Acceso acceso = ReglasAcceso.clasificar(request.getMethod(), path);
        switch (acceso) {
            case INTERNA, NO_EXPUESTA -> {
                if (acceso == Acceso.INTERNA) {
                    log.warn("Intento de acceder a una ruta interna desde el Gateway: {} {}", request.getMethod(), uri);
                }
                ApiErrorJson.escribir(request, response, HttpStatus.NOT_FOUND, NO_ENCONTRADO);
            }
            case PUBLICA -> chain.doFilter(request, response);
            case CLIENTE, ADMINISTRADOR -> {
                UsuarioToken usuario;
                try {
                    usuario = validator.validar(token(request));
                } catch (NoAutenticadoException e) {
                    log.info("401 en el Gateway ({}): {} {}", e.getMessage(), request.getMethod(), uri);
                    ApiErrorJson.escribir(request, response, HttpStatus.UNAUTHORIZED, e.getMessage());
                    return;
                }
                if (acceso == Acceso.ADMINISTRADOR && !usuario.esAdministrador()) {
                    log.info("403 en el Gateway: usuario {} sin rol ADMINISTRADOR en {} {}", usuario.userId(),
                            request.getMethod(), uri);
                    ApiErrorJson.escribir(request, response, HttpStatus.FORBIDDEN, SIN_PERMISO);
                    return;
                }
                chain.doFilter(request, response);
            }
        }
    }

    // Operación auxiliar que forma parte de la responsabilidad de esta clase. Método token (token).
    private static String token(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ") || header.substring(7).isBlank()) {
            throw new NoAutenticadoException(SIN_SESION);
        }
        return header.substring(7).trim();
    }
}
