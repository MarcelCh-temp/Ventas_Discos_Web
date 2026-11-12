/** Modelos locales de demostración. NO son DTO de Catalog ni Orders. */
export type Producto = Readonly<{
  id: string; // Identificador de interfaz `demo:categoria:índice`, NO ID de base de datos.
  nombre: string;
  descripcion: string;
  precioUnitario: number; // CLP; precio visible del HTML original, NO precio confirmado por backend.
  categoria: string;
  iconClassName: string;
}>;

export type ItemCarrito = Readonly<{
  producto: Producto;
  cantidad: number;
}>;

export type CartTotals = Readonly<{ cantidadUnidades: number; subtotal: number }>;
