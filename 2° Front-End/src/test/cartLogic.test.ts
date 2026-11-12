import { describe, expect, it } from 'vitest';
import {
  addToCart,
  updateCartQuantity,
  removeFromCart,
  calculateCartTotals,
  formatCLP,
} from '../state/cartLogic';
import type { ItemCarrito, Producto } from '../models/Cart';

const anime: Producto = {
  id: 'demo:anime:0', nombre: 'Box Set Anime', descripcion: 'Ejemplo',
  precioUnitario: 32990, categoria: 'anime', iconClassName: 'bi bi-play',
};
const musica: Producto = {
  id: 'demo:musica:0', nombre: 'Álbum Vinilo', descripcion: 'Ejemplo',
  precioUnitario: 27990, categoria: 'musica', iconClassName: 'bi bi-disc',
};

describe('Reglas del carrito en TypeScript (datos locales de demostración)', () => {
  it('01: añade un producto con cantidad inicial 1', () => {
    expect(addToCart([], anime)).toEqual([{ producto: anime, cantidad: 1 }]);
  });

  it('02: aumenta la cantidad sin duplicar la línea del mismo producto', () => {
    const once = addToCart([], anime);
    expect(addToCart(once, anime)).toEqual([{ producto: anime, cantidad: 2 }]);
  });

  it('03: conserva líneas distintas al agregar productos diferentes', () => {
    const result = addToCart(addToCart([], anime), musica);
    expect(result.map(i => i.producto.id)).toEqual([anime.id, musica.id]);
  });

  it('04: actualiza la cantidad de la línea seleccionada sin tocar las otras', () => {
    const original: ItemCarrito[] = [{ producto: anime, cantidad: 1 }, { producto: musica, cantidad: 3 }];
    const changed = updateCartQuantity(original, anime.id, 4);
    expect(changed.map(i => i.cantidad)).toEqual([4, 3]);
    expect(original[0].cantidad).toBe(1);
  });

  it.each([0, -1, 1.5, Number.NaN, Number.POSITIVE_INFINITY])(
    '05: rechaza cantidades inválidas (%s)', amount => {
      const original: ItemCarrito[] = [{ producto: anime, cantidad: 2 }];
      expect(updateCartQuantity(original, anime.id, amount)).toEqual(original);
    },
  );

  it('06: elimina solo el artículo seleccionado', () => {
    const items: ItemCarrito[] = [{ producto: anime, cantidad: 1 }, { producto: musica, cantidad: 2 }];
    expect(removeFromCart(items, anime.id)).toEqual([{ producto: musica, cantidad: 2 }]);
  });

  it('07: calcula cero para un carrito vacío', () => {
    expect(calculateCartTotals([])).toEqual({ cantidadUnidades: 0, subtotal: 0 });
  });

  it('08: calcula unidades, subtotal por línea y total para varios productos', () => {
    const items: ItemCarrito[] = [{ producto: anime, cantidad: 2 }, { producto: musica, cantidad: 1 }];
    expect(calculateCartTotals(items)).toEqual({ cantidadUnidades: 3, subtotal: 93970 });
  });

  it('09: rechaza precios negativos y cantidades no enteras', () => {
    expect(() => calculateCartTotals([{ producto: { ...anime, precioUnitario: -1 }, cantidad: 1 }])).toThrow();
    expect(() => calculateCartTotals([{ producto: anime, cantidad: 1.5 }])).toThrow();
  });

  it('10: rechaza desbordamiento numérico en el total', () => {
    expect(() => calculateCartTotals([{ producto: anime, cantidad: Number.MAX_SAFE_INTEGER }])).toThrow();
  });

  it('11: no altera el arreglo de origen al añadir o remover', () => {
    const initial: ItemCarrito[] = [{ producto: anime, cantidad: 1 }];
    const added = addToCart(initial, musica);
    const removed = removeFromCart(added, anime.id);
    expect(initial).toHaveLength(1);
    expect(added).toHaveLength(2);
    expect(removed).toEqual([{ producto: musica, cantidad: 1 }]);
  });

  it('12: presenta precios en pesos chilenos sin decimales', () => {
    expect(formatCLP(32990)).toContain('32.990');
  });
});
