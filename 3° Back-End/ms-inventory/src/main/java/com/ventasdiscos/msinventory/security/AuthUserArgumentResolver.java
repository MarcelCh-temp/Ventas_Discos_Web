package com.ventasdiscos.msinventory.security;

import com.ventasdiscos.msinventory.exception.NoAutenticadoException;
import lombok.RequiredArgsConstructor;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * Si un método del Controller recibe un parámetro AuthUser, este resolver lo
 * construye desde el header "Authorization: Bearer <token>".
 * Para qué: el userId/rol SIEMPRE sale del token firmado, nunca de la URL o del body
 * (RNF-06, RNF-07, evita IDOR). Sin token válido -> 401.
 */
@Component
@RequiredArgsConstructor
public class AuthUserArgumentResolver implements HandlerMethodArgumentResolver {

    private final JwtService jwtService;

    @Override
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
