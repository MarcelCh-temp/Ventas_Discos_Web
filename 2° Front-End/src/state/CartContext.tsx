import { createContext, useContext, useEffect, useMemo, useState, type ReactNode } from 'react';
import type { ItemCarrito, Producto } from '../models/Cart';
import { getDemoProduct } from '../data/demoCatalog';
import { addToCart, calculateCartTotals, removeFromCart, updateCartQuantity } from './cartLogic';

// Sesión de demo: sólo ID locales y cantidades; nunca credenciales ni pedidos reales.
const STORAGE_KEY = 'nackson:carrito-demo:v1';

type StoredItem = { id: string; cantidad: number };
function readDemoCart(): ItemCarrito[] {
  try {
    const raw = sessionStorage.getItem(STORAGE_KEY);
    if (!raw) return [];
    const decoded: unknown = JSON.parse(raw);
    if (!Array.isArray(decoded)) return [];
    const result: ItemCarrito[] = [];
    const seen = new Set<string>();
    for (const entry of decoded) {
      if (typeof entry !== 'object' || entry === null) continue;
      const item = entry as Partial<StoredItem>;
      if (typeof item.id !== 'string' || !Number.isSafeInteger(item.cantidad) ||
          (item.cantidad as number) < 1 || seen.has(item.id)) continue;
      const producto = getDemoProduct(item.id);
      if (!producto) continue;
      result.push({ producto, cantidad: item.cantidad as number });
      seen.add(item.id);
    }
    try { calculateCartTotals(result); } catch { return []; }
    return result;
  } catch { return []; }
}

type CartContextValue = {
  items: readonly ItemCarrito[];
  subtotal: number;
  unidades: number;
  add: (product: Producto) => void;
  updateQuantity: (productId: string, amount: number) => void;
  remove: (productId: string) => void;
  clear: () => void;
};
const CartContext = createContext<CartContextValue | null>(null);

export function CartProvider({ children }: { children: ReactNode }) {
  const [items, setItems] = useState<ItemCarrito[]>(readDemoCart);
  useEffect(() => {
    try {
      const snapshot: StoredItem[] = items.map(item => ({ id: item.producto.id, cantidad: item.cantidad }));
      sessionStorage.setItem(STORAGE_KEY, JSON.stringify(snapshot));
    } catch { /* El carrito sigue funcionando sin almacenamiento de sesión. */ }
  }, [items]);

  const totals = useMemo(() => calculateCartTotals(items), [items]);
  const value = useMemo<CartContextValue>(() => ({
    items, subtotal: totals.subtotal, unidades: totals.cantidadUnidades,
    add: product => setItems(current => addToCart(current, product)),
    updateQuantity: (id, amount) => setItems(current => updateCartQuantity(current, id, amount)),
    remove: id => setItems(current => removeFromCart(current, id)),
    clear: () => setItems([]),
  }), [items, totals.subtotal, totals.cantidadUnidades]);
  return <CartContext.Provider value={value}>{children}</CartContext.Provider>;
}

export function useCart(): CartContextValue {
  const ctx = useContext(CartContext);
  if (!ctx) throw new Error('useCart requiere CartProvider');
  return ctx;
}
