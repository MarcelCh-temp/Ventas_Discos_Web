import { beforeEach, describe, expect, it, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import RealCheckoutPage from '../pages/RealCheckoutPage';
import { MemoryRouter } from 'react-router-dom';
import { HttpFailure } from '../services/http';

const mocks = vi.hoisted(() => ({
  confirm: vi.fn(),
  validate: vi.fn(),
}));

vi.mock('../state/AuthContext', () => ({
  useAuth: () => ({
    session: { token: 'TOKEN_PRUEBA', userId: 101, rol: 'CLIENTE' },
  }),
}));

vi.mock('../components/ConfirmDialog', () => ({
  useAppDialog: () => ({
    confirm: async () => true,
  }),
}));

vi.mock('../components/StoreLayout', () => ({
  StoreLayout: ({ children }: { children: React.ReactNode }) => (
    <main>{children}</main>
  ),
}));

vi.mock('../components/StoreNotice', () => ({
  StoreNotice: ({ error }: { error: string }) =>
    error ? <div role="alert">{error}</div> : null,
}));

vi.mock('../services/storeApi', () => ({
  clp: (value: number) => String(value),
  errorMessage: (error: Error) => error.message,
  storeApi: {
    cart: {
      read: async () => ({
        vacio: false,
        items: [],
        total: 4500,
      }),
    },
    checkout: {
      deliveries: async () => [{
        codigo: 'RETIRO_TIENDA',
        nombre: 'Retiro en tienda',
        camposObligatorios: [],
        camposOpcionales: [],
      }],
      payments: async () => ({
        metodos: [{ codigo: 'WEBPAY', nombre: 'Webpay simulado' }],
        resultadosSimulados: ['APROBADO'],
        aviso: 'Pago académico simulado',
      }),
      validate: mocks.validate,
      confirm: mocks.confirm,
    },
  },
}));

describe('Checkout real: resultado incierto de una compra', () => {
  beforeEach(() => {
    vi.clearAllMocks();

    mocks.validate.mockResolvedValue({
      listo: true,
      total: 4500,
      descuento: 0,
      costoEnvio: 0,
      premioRuleta: 'NINGUNO',
      problemas: {},
    });

    mocks.confirm.mockRejectedValue(
      new HttpFailure(0, 'Se perdió la conexión con el servidor')
    );
  });

  it('advierte que la compra podría haberse registrado', async () => {
    const user = userEvent.setup();
    render(<MemoryRouter><RealCheckoutPage /></MemoryRouter>);

    await user.click(
      await screen.findByRole('button', {
        name: /validar precio, stock y entrega/i,
      })
    );

    await user.click(
      await screen.findByRole('button', {
        name: /confirmar pago simulado/i,
      })
    );

    expect(mocks.confirm).toHaveBeenCalledTimes(1);

    expect(
      await screen.findByText(/no pudimos comprobar el resultado de tu compra/i)
    ).toBeInTheDocument();

    expect(
      screen.getByRole('link', { name: /consultar mis pedidos/i })
    ).toHaveAttribute('href', '/pedidos');
  });

  it('muestra rechazo HTTP sin advertencia de compra incierta', async () => {
    mocks.confirm.mockRejectedValue(
      new HttpFailure(409, 'Tu carrito cambió; revisa el resumen')
    );

    const user = userEvent.setup();
    render(<MemoryRouter><RealCheckoutPage /></MemoryRouter>);

    await user.click(
      await screen.findByRole('button', {
        name: /validar precio, stock y entrega/i,
      })
    );

    await user.click(
      await screen.findByRole('button', {
        name: /confirmar pago simulado/i,
      })
    );

    expect(
      await screen.findByRole('alert')
    ).toHaveTextContent(/tu carrito cambió/i);

    expect(
      screen.queryByText(/no pudimos comprobar el resultado de tu compra/i)
    ).not.toBeInTheDocument();

    expect(
      screen.queryByRole('link', { name: /consultar mis pedidos/i })
    ).not.toBeInTheDocument();
  });
});
