package ms_soporte.ms_soporte.dto;

/** Lote 5 · Contrato INTERNO para Accounts (HU-78, ERS 13.8): ¿el usuario tiene tickets ABIERTA o EN_PROCESO? */
// Ejecuta existe response como parte de las responsabilidades de esta clase.
public record ExisteResponse(Long userId, boolean existe) {
}
