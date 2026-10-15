package com.ventasdiscos.msaccounts.exception;

import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Cada tipo de error se traduce al código HTTP y JSON correctos (RNF-04, RNF-16). */
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();
    private final MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/prueba");

    @SuppressWarnings("unused")
    void metodoDePrueba(String dato) {
    }

    @Test
    void excepcionDeNegocioUsaSuCodigoYErrores() {
        ResponseEntity<ApiError> r = handler.handleApi(
                new ValidacionNegocioException("Revisa los datos", Map.of("campo", "inválido")), request);
        assertEquals(400, r.getStatusCode().value());
        assertEquals("Revisa los datos", r.getBody().mensaje());
        assertEquals("inválido", r.getBody().errores().get("campo"));
        assertEquals("/api/v1/prueba", r.getBody().path());
    }

    @Test
    void validacionDelDtoDevuelveCampoYMensaje() throws Exception {
        BeanPropertyBindingResult br = new BeanPropertyBindingResult(new Object(), "req");
        br.addError(new FieldError("req", "nombre", "El nombre es obligatorio"));
        br.addError(new FieldError("req", "nombre", "Segundo error del mismo campo"));
        MethodParameter param = new MethodParameter(
                GlobalExceptionHandlerTest.class.getDeclaredMethod("metodoDePrueba", String.class), 0);

        ResponseEntity<ApiError> r = handler.handleValidacion(new MethodArgumentNotValidException(param, br), request);

        assertEquals(400, r.getStatusCode().value());
        assertEquals("El nombre es obligatorio", r.getBody().errores().get("nombre"));
    }

    @Test
    void jsonMalFormadoDa400() {
        ResponseEntity<ApiError> r = handler.handleJson(
                new HttpMessageNotReadableException("mal", new MockHttpInputMessage(new byte[0])), request);
        assertEquals(400, r.getStatusCode().value());
        assertNull(r.getBody().errores());
    }

    @Test
    void parametroConTipoIncorrectoDa400() {
        MethodArgumentTypeMismatchException ex = mock(MethodArgumentTypeMismatchException.class);
        when(ex.getName()).thenReturn("id");
        ResponseEntity<ApiError> r = handler.handleTipo(ex, request);
        assertEquals(400, r.getStatusCode().value());
        assertEquals("El parámetro 'id' tiene un valor inválido", r.getBody().mensaje());
    }

    @Test
    void violacionDeIntegridadDa409() {
        ResponseEntity<ApiError> r = handler.handleIntegridad(
                new DataIntegrityViolationException("dup", new RuntimeException("Duplicate entry")), request);
        assertEquals(409, r.getStatusCode().value());
    }

    @Test
    void errorDeSpringMvcConservaSuCodigo() {
        ErrorResponseException sinDetalle = new ErrorResponseException(HttpStatus.METHOD_NOT_ALLOWED);
        sinDetalle.getBody().setDetail(null);
        ResponseEntity<ApiError> r1 = handler.handleGeneral(sinDetalle, request);
        assertEquals(405, r1.getStatusCode().value());
        assertEquals("Method Not Allowed", r1.getBody().mensaje());

        ErrorResponseException conDetalle = new ErrorResponseException(HttpStatus.NOT_FOUND);
        conDetalle.setDetail("Ruta inexistente");
        ResponseEntity<ApiError> r2 = handler.handleGeneral(conDetalle, request);
        assertEquals(404, r2.getStatusCode().value());
        assertEquals("Ruta inexistente", r2.getBody().mensaje());
    }

    @Test
    void errorNoPrevistoDa500ConMensajeSeguro() {
        ResponseEntity<ApiError> r = handler.handleGeneral(new IllegalStateException("detalle interno"), request);
        assertEquals(500, r.getStatusCode().value());
        assertEquals("Ocurrió un error inesperado. Intenta nuevamente", r.getBody().mensaje());
    }

    @Test
    void cadaExcepcionTieneSuCodigo() {
        assertEquals(HttpStatus.UNAUTHORIZED, new NoAutenticadoException("x").getStatus());
        assertEquals(HttpStatus.FORBIDDEN, new AccesoDenegadoException("x").getStatus());
        assertEquals(HttpStatus.BAD_REQUEST, new ValidacionNegocioException("x").getStatus());
    }
}
