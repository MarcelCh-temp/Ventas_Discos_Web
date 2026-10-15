package com.ventasdiscos.mscatalog.dto;

import java.util.List;

/** HU-10: informa cuáles se retiraron y cuáles ya estaban retirados (no se repite la operación). */
public record RetiroResponse(
        List<Long> retirados,
        List<Long> yaRetirados,
        List<Long> noEncontrados
) {
}
