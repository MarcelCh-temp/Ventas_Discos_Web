package com.ventasdiscos.mspostsale.security;

import com.ventasdiscos.mspostsale.exception.NoAutenticadoException;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * Lote 4 (copia de ms-ordenes, Lote 3): si un método del Controller recibe AuthUser, se arma desde "Authorization: Bearer <token>".
 * Así el usuarioId SIEMPRE sale del token y no de la URL (antes: ?usuarioId= cualquiera).
 */
@Component
public class AuthUserArgumentResolver implements HandlerMethodArgumentResolver {

    // Dependencia inyectada para acceder a jwt service sin crearla manualmente.
    private final JwtService jwtService;

    // Ejecuta auth user argument resolver como parte de las responsabilidades de esta clase.
    public AuthUserArgumentResolver(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    // Ejecuta supports parameter como parte de las responsabilidades de esta clase.
    public boolean supportsParameter(MethodParameter parameter) {
        return AuthUser.class.equals(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        String header = webRequest.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            throw new NoAutenticadoException("Debes iniciar sesión para acceder a este recurso");
        }
        return jwtService.validar(header.substring(7).trim());
    }
}
