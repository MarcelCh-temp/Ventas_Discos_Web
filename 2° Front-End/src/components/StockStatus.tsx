import type { Producto } from '../services/storeApi';

/** Solo muestra el stock confirmado del servidor, nunca un valor supuesto. */
export function StockStatus({ product }: { product: Producto }) {
  const stock = product.stockActual;
  if (stock == null) return <p className="small text-info mb-2">{product.disponibilidad.replace('_', ' ')} · Stock por confirmar</p>;
  if (!product.comprable || stock <= 0) return <p className="small text-danger mb-2">Agotado / no disponible</p>;
  if (product.disponibilidad === 'STOCK_CRITICO') return <p className="small text-warning mb-2">¡Queda poco stock! Solo {stock} {stock === 1 ? 'unidad disponible' : 'unidades disponibles'}</p>;
  return <p className="small text-info mb-2">{stock} {stock === 1 ? 'unidad disponible' : 'unidades disponibles'}</p>;
}
