package com.ventasdiscos.mspostsale.dto;

import org.springframework.data.domain.Page;

import java.util.List;

/** Lote 4: paginación con los mismos nombres que Catalog, Soporte y Orders (RF-POS-05: vista con gran volumen). */
public record PageResponse<T>(List<T> contenido, int pagina, int tamano, long totalElementos, int totalPaginas) {

    // Ejecuta of como parte de las responsabilidades de esta clase.
    public static <T> PageResponse<T> of(Page<?> page, List<T> contenido) {
        return new PageResponse<>(contenido, page.getNumber(), page.getSize(), page.getTotalElements(),
                page.getTotalPages());
    }
}
