package ms_soporte.ms_soporte.security;

import ms_soporte.ms_soporte.exception.NoAutenticadoException;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * Lote 1: si un método del Controller recibe AuthUser, se arma desde "Authorization: Bearer <token>".
 * Así el usuarioId SIEMPRE sale del token y no de la URL (antes: ?usuarioId= cualquiera).
 */
@Component
public class AuthUserArgumentResolver implements HandlerMethodArgumentResolver {

    private final JwtService jwtService;

    public AuthUserArgumentResolver(JwtService jwtService) {
        this.jwtService = jwtService;
    }

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
