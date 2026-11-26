import { storeApi, type Producto } from '../services/storeApi';
/** Carrito temporal de un visitante: IDs y cantidades solamente, sin datos de cuenta ni precios confiables. */
const KEY = 'nackson:carrito-visitante:v1';
export type GuestItem = { productId: number; cantidad: number };
export function guestItems(): GuestItem[] {
  try {
    const data: unknown = JSON.parse(sessionStorage.getItem(KEY) || '[]');
    if (!Array.isArray(data)) return [];
    const items = new Map<number,GuestItem>();
    for (const item of data) if (item && Number.isSafeInteger(item.productId) && item.productId > 0 && Number.isSafeInteger(item.cantidad) && item.cantidad > 0 && item.cantidad <= 200) items.set(item.productId,{productId:item.productId,cantidad:item.cantidad});
    return [...items.values()];
  } catch { return []; }
}
function save(items: GuestItem[]) {
  sessionStorage.setItem(KEY,JSON.stringify(items));
  window.dispatchEvent(new Event('nackson-cart-changed'));
}
export function removeGuest(id: number) { save(guestItems().filter(i=>i.productId!==id)); }
export function clearGuest() { save([]); }
export async function setGuest(id: number, cantidad: number): Promise<Producto> {
  if (!Number.isSafeInteger(cantidad)||cantidad<1||cantidad>200) throw new Error('Cantidad fuera del rango permitido (1 a 200).');
  const actual = await storeApi.catalog.detail(id);
  if (!actual.comprable || (actual.stockActual!==null&&cantidad>actual.stockActual)) throw new Error('El producto no está disponible en la cantidad solicitada.');
  const items = guestItems().filter(i=>i.productId!==id);
  save([...items,{productId:id,cantidad}]);
  return actual;
}
export async function addGuest(id: number, cantidad: number) {
  return setGuest(id,(guestItems().find(i=>i.productId===id)?.cantidad??0)+cantidad);
}
