/** Contrato documentado por el equipo, Lote 1; aún no verificado en XAMPP. */
export interface ApiError {
  timestamp?: string;
  status: number;
  error?: string;
  mensaje: string;
  path?: string;
  errores?: Record<string, string> | null;
  correlationId?: string;
}
