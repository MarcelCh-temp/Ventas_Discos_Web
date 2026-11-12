package ms_ordenes.ms_ordenes.dto;

import ms_ordenes.ms_ordenes.model.MetodoEntrega;

import java.util.List;

/** RF-COM-05: lo publica el backend para que el frontend no mantenga una lista fija. */
public record MetodoEntregaResponse(MetodoEntrega codigo, String nombre, List<String> camposObligatorios,
                                    List<String> camposOpcionales) {

    public static MetodoEntregaResponse from(MetodoEntrega m) {
        return new MetodoEntregaResponse(m, m.getNombre(), m.camposObligatorios(), m.camposOpcionales());
    }
}
