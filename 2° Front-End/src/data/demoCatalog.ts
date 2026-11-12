import { categoryProductPreviews } from './productPreviews';
import type { Producto } from '../models/Cart';

/** Sólo reutiliza los ocho ejemplos visuales ya existentes en las páginas de categorías. */
export type DemoCategory = keyof typeof categoryProductPreviews;

export function parseVisibleCLP(text: string): number {
  if (!/^\$\d{1,3}(?:\.\d{3})*$/.test(text)) {
    throw new Error('Precio de ejemplo inválido: ' + text);
  }
  const price = Number(text.slice(1).replace(/\./g, ''));
  if (!Number.isSafeInteger(price) || price <= 0) throw new Error('Precio de ejemplo inválido');
  return price;
}

/** Los ID `demo:` son claves locales de React; jamás deben enviarse a un microservicio. */
export const demoProducts: readonly Producto[] = (
  Object.entries(categoryProductPreviews) as [DemoCategory, (typeof categoryProductPreviews)[DemoCategory]][]
).flatMap(([category, previews]) => previews.map((preview, index) => ({
  id: `demo:${category}:${index}`,
  nombre: preview.title,
  descripcion: preview.description,
  precioUnitario: parseVisibleCLP(preview.priceText),
  categoria: category,
  iconClassName: preview.iconClassName,
})));

const productMap = new Map(demoProducts.map(p => [p.id, p]));
export function getDemoProduct(id: string): Producto | undefined {
  return productMap.get(id);
}
