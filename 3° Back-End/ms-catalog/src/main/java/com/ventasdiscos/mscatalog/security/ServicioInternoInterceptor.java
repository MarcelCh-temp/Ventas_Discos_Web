package com.ventasdiscos.mscatalog.security;

import com.ventasdiscos.mscatalog.exception.NoAutenticadoException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Arrays;
import java.util.List;

/**
 * Lote 6 · DOC-05: protege las rutas internas (/api/v1/internal/**) con el JWT de servicio.
 * Una ruta se considera interna si su URL empieza con /api/v1/internal/, si su controller está mapeado bajo
 * /internal o si el método tiene {@link ServiciosPermitidos} (así una URL escrita de otra forma, por ejemplo con
 * caracteres codificados, no se salta la validación). Para esas rutas:
 * <ul>
 *   <li>Sin "Authorization: Bearer" o con un token que no es de servicio → 401.</li>
 *   <li>Token de servicio válido pero emisor no autorizado (o ruta sin anotación) → 403.</li>
 *   <li>Si pasa, deja el emisor autenticado en {@link #ATRIBUTO_EMISOR} (lo usa Catalog para DOC-08).</li>
 * </ul>
 * Las rutas de clientes y administradores no cambian: siguen con el JWT de usuario (AuthUser).
 * La medida complementaria server.address=127.0.0.1 se mantiene.
 */
public class ServicioInternoInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(ServicioInternoInterceptor.class);
    public static final String ATRIBUTO_EMISOR = ServicioInternoInterceptor.class.getName() + ".emisor";
    static final String SIN_TOKEN = "Esta operación interna requiere autenticación de servicio";

    private final ObjectProvider<ServicioJwtService> servicioJwt;

    public ServicioInternoInterceptor(ObjectProvider<ServicioJwtService> servicioJwt) {
        this.servicioJwt = servicioJwt;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod metodo)) {
            return true;
        }
        ServiciosPermitidos regla = metodo.getMethodAnnotation(ServiciosPermitidos.class);
        if (regla == null) {
            regla = metodo.getBeanType().getAnnotation(ServiciosPermitidos.class);
        }
        boolean interna = regla != null || esRutaInterna(request) || esControladorInterno(metodo);
        if (!interna) {
            return true;
        }
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ") || header.length() <= 7) {
            log.warn("Llamada interna sin token de servicio: {} {}", request.getMethod(), request.getRequestURI());
            throw new NoAutenticadoException(SIN_TOKEN);
        }
        ServicioJwtService jwt = servicioJwt.getIfAvailable();
        if (jwt == null) {
            throw new NoAutenticadoException(SIN_TOKEN);
        }
        List<String> permitidos = regla == null ? List.of() : Arrays.asList(regla.value());
        if (regla == null) {
            log.warn("Ruta interna sin @ServiciosPermitidos: {} {} (se rechaza)", request.getMethod(), request.getRequestURI());
        }
        try {
            String emisor = jwt.validar(header.substring(7).trim(), permitidos);
            request.setAttribute(ATRIBUTO_EMISOR, emisor);
            log.debug("Llamada interna autenticada de {}: {} {}", emisor, request.getMethod(), request.getRequestURI());
            return true;
        } catch (RuntimeException e) {
            // Nunca se registra el token, solo el motivo.
            log.warn("Llamada interna rechazada ({}): {} {}", e.getMessage(), request.getMethod(), request.getRequestURI());
            throw e;
        }
    }

    static boolean esRutaInterna(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String contexto = request.getContextPath();
        String path = contexto != null && !contexto.isEmpty() && uri.startsWith(contexto) ? uri.substring(contexto.length()) : uri;
        return path.startsWith(Servicios.PREFIJO_INTERNO) || path.equals("/api/v1/internal");
    }

    private static boolean esControladorInterno(HandlerMethod metodo) {
        RequestMapping mapping = metodo.getBeanType().getAnnotation(RequestMapping.class);
        if (mapping == null) {
            return false;
        }
        for (String p : mapping.value()) {
            if (p.contains("/internal/") || p.endsWith("/internal")) {
                return true;
            }
        }
        for (String p : mapping.path()) {
            if (p.contains("/internal/") || p.endsWith("/internal")) {
                return true;
            }
        }
        return false;
    }
}
