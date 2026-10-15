package com.ventasdiscos.mscatalog.dto;

import com.ventasdiscos.mscatalog.exception.ValidacionNegocioException;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** RNF-11: página y tamaño válidos; tamaño máximo 100. */
class PaginacionTest {

    @Test
    void paginaValida() {
        PageRequest pr = Paginacion.of(2, 50, Sort.unsorted());
        assertEquals(2, pr.getPageNumber());
        assertEquals(50, pr.getPageSize());
    }

    @Test
    void paginaNegativaSeRechaza() {
        assertThrows(ValidacionNegocioException.class, () -> Paginacion.of(-1, 20, Sort.unsorted()));
    }

    @Test
    void tamanoFueraDeRangoSeRechaza() {
        assertThrows(ValidacionNegocioException.class, () -> Paginacion.of(0, 0, Sort.unsorted()));
        assertThrows(ValidacionNegocioException.class, () -> Paginacion.of(0, 101, Sort.unsorted()));
    }
}
