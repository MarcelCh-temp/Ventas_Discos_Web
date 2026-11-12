package ms_ordenes.ms_ordenes.dto;

/** Lote 5 · Contrato INTERNO para Accounts (HU-78, ERS 13.8): ¿el usuario tiene pedidos activos? */
public record ExisteResponse(Long userId, boolean existe) {
}
