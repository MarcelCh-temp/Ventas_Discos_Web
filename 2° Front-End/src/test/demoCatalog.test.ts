import { describe, expect, it } from 'vitest';
import { demoProducts, getDemoProduct, parseVisibleCLP } from '../data/demoCatalog';

describe('Datos de ejemplo del catálogo (sin backend)', () => {
  it('13: interpreta precios CLP visibles correctamente', () => {
    expect(parseVisibleCLP('$32.990')).toBe(32990);
    expect(parseVisibleCLP('$1.000')).toBe(1000);
  });

  it.each(['32.990', '$-1', '$32,990', '$0', '$abc'])(
    '14: rechaza formatos de precios de ejemplo inválidos (%s)', amount => {
      expect(() => parseVisibleCLP(amount)).toThrow();
    },
  );

  it('15: usa ID locales de demostración y no inventa productos de API', () => {
    expect(demoProducts.length).toBeGreaterThan(0);
    for (const product of demoProducts) {
      expect(product.id).toMatch(/^demo:/);
      expect(getDemoProduct(product.id)).toBe(product);
    }
    expect(getDemoProduct('producto-que-no-existe')).toBeUndefined();
  });
});
