import { defineConfig } from 'vitest/config';
import react from '@vitejs/plugin-react-swc';

export default defineConfig({
  plugins: [react()],
  test: {
    environment: 'jsdom',
    globals: true,
    setupFiles: ['./src/test/setup.ts'],
    clearMocks: true,
    restoreMocks: true,
    css: false,
    coverage: {
      provider: 'v8',
      reporter: ['text', 'html'],
      reportsDirectory: './coverage',
      include: [
        'src/state/cartLogic.ts',
        'src/state/CartContext.tsx',
        'src/data/demoCatalog.ts',
        'src/pages/CarritoPage.tsx',
        'src/pages/CheckoutPage.tsx',
        'src/state/authValidation.ts',
        'src/services/http.ts',
        'src/services/accountsApi.ts',
        'src/state/AuthContext.tsx',
        'src/pages/LoginPage.tsx',
        'src/pages/RegistroPage.tsx',
        'src/pages/PerfilPage.tsx',
        'src/services/storeApi.ts',
        'src/components/RealCatalog.tsx',
        'src/pages/RealCarritoPage.tsx',
        'src/pages/RealCheckoutPage.tsx',
        'src/pages/SoportePage.tsx',
        'src/pages/PedidosPage.tsx',
        'src/pages/NotificacionesPage.tsx',
        'src/pages/PostVentaPage.tsx',
        'src/pages/RealAdminPage.tsx',
      ],
    },
  },
});
