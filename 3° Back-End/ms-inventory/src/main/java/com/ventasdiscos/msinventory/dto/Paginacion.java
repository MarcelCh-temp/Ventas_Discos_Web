package com.ventasdiscos.msinventory.dto;

import com.ventasdiscos.msinventory.exception.ValidacionNegocioException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

/** Valida page/size antes de consultar (RNF-11: limitar size evita respuestas gigantes). */
public final class Paginacion {

    public static final int TAMANO_MAXIMO = 100;

    private Paginacion() {
    }

    public static PageRequest of(int pagina, int tamano, Sort orden) {
        if (pagina < 0) {
            throw new ValidacionNegocioException("La página debe ser 0 o mayor");
        }
        if (tamano < 1 || tamano > TAMANO_MAXIMO) {
            throw new ValidacionNegocioException("El tamaño de página debe estar entre 1 y " + TAMANO_MAXIMO);
        }
        return PageRequest.of(pagina, tamano, orden);
    }
}
