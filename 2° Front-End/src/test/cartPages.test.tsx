import { beforeEach, describe, expect, it } from 'vitest';
import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { CartProvider } from '../state/CartContext';
import CarritoPage from '../pages/CarritoPage';
import CheckoutPage from '../pages/CheckoutPage';
import { demoProducts } from '../data/demoCatalog';
import { formatCLP } from '../state/cartLogic';

const key = 'nackson:carrito-demo:v1';
const itemA = demoProducts[0];
const itemB = demoProducts[1];

function renderPages(start = '/carrito.html') {
  return render(
    <MemoryRouter initialEntries={[start]}>
      <CartProvider>
        <Routes>
          <Route path="/carrito.html" element={<CarritoPage />} />
          <Route path="/checkout.html" element={<CheckoutPage />} />
        </Routes>
      </CartProvider>
    </MemoryRouter>,
  );
}

function seed(items: Array<{ id: string; cantidad: number }>) {
  sessionStorage.setItem(key, JSON.stringify(items));
}

beforeEach(() => {
  sessionStorage.clear();
});

describe('Interfaz React del carrito y checkout (sin compras reales)', () => {
  it('16: muestra carrito vacío con checkout deshabilitado', () => {
    renderPages();
    expect(screen.getByText(/tu carrito está vacío/i)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /revisar checkout/i })).toBeDisabled();
  });

  it('17: lee de sessionStorage las cantidades de los ID de demo', () => {
    seed([{ id: itemA.id, cantidad: 2 }]);
    renderPages();
    expect(screen.getByText(itemA.nombre)).toBeInTheDocument();
    expect(screen.getByRole('spinbutton', { name: `Cantidad de ${itemA.nombre}` })).toHaveValue(2);
    expect(screen.getAllByText(formatCLP(itemA.precioUnitario * 2)).length).toBeGreaterThan(0);
  });

  it('18: incrementa y decrementa cantidades con React, actualizando las unidades', async () => {
    seed([{ id: itemA.id, cantidad: 1 }]);
    const user = userEvent.setup();
    renderPages();
    const quantity = screen.getByRole('spinbutton', { name: `Cantidad de ${itemA.nombre}` });
    await user.click(screen.getByRole('button', { name: `Aumentar cantidad de ${itemA.nombre}` }));
    expect(quantity).toHaveValue(2);
    await user.click(screen.getByRole('button', { name: `Disminuir cantidad de ${itemA.nombre}` }));
    expect(quantity).toHaveValue(1);
    expect(screen.getByRole('button', { name: `Disminuir cantidad de ${itemA.nombre}` })).toBeDisabled();
  });

  it('19: elimina una sola línea dejando las otras intactas', async () => {
    seed([{ id: itemA.id, cantidad: 1 }, { id: itemB.id, cantidad: 1 }]);
    const user = userEvent.setup();
    renderPages();
    await user.click(screen.getByRole('button', { name: `Eliminar ${itemA.nombre} del carrito` }));
    expect(screen.queryByText(itemA.nombre)).not.toBeInTheDocument();
    expect(screen.getByText(itemB.nombre)).toBeInTheDocument();
  });

  it('20: vacía el carrito y deshabilita el checkout', async () => {
    seed([{ id: itemA.id, cantidad: 1 }, { id: itemB.id, cantidad: 1 }]);
    const user = userEvent.setup();
    renderPages();
    await user.click(screen.getByRole('button', { name: /vaciar carrito/i }));
    expect(screen.getByText(/tu carrito está vacío/i)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /revisar checkout/i })).toBeDisabled();
  });

  it('21: navega al checkout con resumen de cantidades y total provisional', async () => {
    seed([{ id: itemA.id, cantidad: 3 }, { id: itemB.id, cantidad: 1 }]);
    const user = userEvent.setup();
    renderPages();
    await user.click(screen.getByRole('link', { name: /revisar checkout/i }));
    expect(screen.getByRole('heading', { name: /finalizar compra/i })).toBeInTheDocument();
    expect(screen.getByText(itemA.nombre)).toBeInTheDocument();
    expect(screen.getByText(itemB.nombre)).toBeInTheDocument();
    expect(screen.getByText(`3 × ${formatCLP(itemA.precioUnitario)}`)).toBeInTheDocument();
    const paymentButton = screen.getByRole('button', { name: /pago pendiente de integración/i });
    expect(paymentButton).toBeDisabled();
    const total = itemA.precioUnitario * 3 + itemB.precioUnitario;
    expect(screen.getByText(formatCLP(total))).toBeInTheDocument();
  });

  it('22: ignora entradas inválidas en sessionStorage en vez de mostrar artículos falsos', () => {
    seed([{ id: itemA.id, cantidad: 0 }, { id: 'id-inventado', cantidad: 5 }]);
    renderPages();
    expect(screen.getByText(/tu carrito está vacío/i)).toBeInTheDocument();
  });

  it('23: muestra una línea por producto incluso si la sesión contenía duplicados', () => {
    seed([{ id: itemA.id, cantidad: 1 }, { id: itemA.id, cantidad: 8 }]);
    renderPages();
    const table = screen.getByRole('table');
    expect(within(table).getAllByText(itemA.nombre)).toHaveLength(1);
  });

  it('24: permite regresar del checkout al carrito sin modificar el subtotal', async () => {
    seed([{ id: itemA.id, cantidad: 1 }]);
    const user = userEvent.setup();
    renderPages('/checkout.html');
    await user.click(screen.getByRole('link', { name: /editar carrito/i }));
    expect(screen.getByRole('spinbutton', { name: `Cantidad de ${itemA.nombre}` })).toHaveValue(1);
    expect(screen.getAllByText(formatCLP(itemA.precioUnitario)).length).toBeGreaterThan(0);
  });
});
