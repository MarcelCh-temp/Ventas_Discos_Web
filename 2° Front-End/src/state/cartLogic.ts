import type { CartTotals, ItemCarrito, Producto } from '../models/Cart';

/** Funciones puras. No consultan stock, precios externos ni modifican el backend. */
export function addToCart(items: readonly ItemCarrito[], product: Producto): ItemCarrito[] {
  const found = items.find(item => item.producto.id === product.id);
  if (found) {
    if (!Number.isSafeInteger(found.cantidad + 1)) return [...items];
    return items.map(item => item.producto.id === product.id
      ? { ...item, cantidad: item.cantidad + 1 } : item);
  }
  return [...items, { producto: product, cantidad: 1 }];
}

export function updateCartQuantity(items: readonly ItemCarrito[], productId: string, amount: number): ItemCarrito[] {
  if (!Number.isSafeInteger(amount) || amount < 1) return [...items];
  return items.map(item => item.producto.id === productId
    ? { ...item, cantidad: amount } : item);
}

export function removeFromCart(items: readonly ItemCarrito[], productId: string): ItemCarrito[] {
  return items.filter(item => item.producto.id !== productId);
}

export function calculateCartTotals(items: readonly ItemCarrito[]): CartTotals {
  return items.reduce<CartTotals>((acc, item) => {
    const { precioUnitario } = item.producto;
    if (!Number.isSafeInteger(item.cantidad) || item.cantidad < 1 ||
        !Number.isSafeInteger(precioUnitario) || precioUnitario < 0) {
      throw new Error('Datos no válidos en carrito de demostración');
    }
    const partial = precioUnitario * item.cantidad;
    const subtotal = acc.subtotal + partial;
    const cantidadUnidades = acc.cantidadUnidades + item.cantidad;
    if (!Number.isSafeInteger(partial) || !Number.isSafeInteger(subtotal) ||
        !Number.isSafeInteger(cantidadUnidades)) {
      throw new Error('Cantidad demasiado grande para el cálculo de ejemplo');
    }
    return { subtotal, cantidadUnidades };
  }, { subtotal: 0, cantidadUnidades: 0 });
}

export function formatCLP(amount: number): string {
  return new Intl.NumberFormat('es-CL', { style: 'currency', currency: 'CLP',
    maximumFractionDigits: 0 }).format(amount);
}
