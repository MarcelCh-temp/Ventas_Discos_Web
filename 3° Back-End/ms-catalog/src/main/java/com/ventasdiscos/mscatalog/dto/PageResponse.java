package com.ventasdiscos.mscatalog.dto;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

/**
 * Respuesta paginada propia (RNF-11: listados con cientos/miles de registros).
 * Se usa en vez de serializar Page<T> directamente para tener un contrato estable.
 */
public record PageResponse<T>(
        List<T> contenido,
        int pagina,
        int tamano,
        long totalElementos,
        int totalPaginas
) {
    public static <E, T> PageResponse<T> of(Page<E> page, Function<E, T> mapper) {
        return new PageResponse<>(page.getContent().stream().map(mapper).toList(),
                page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }

    public static <T> PageResponse<T> of(Page<?> page, List<T> contenido) {
        return new PageResponse<>(contenido, page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }
}
