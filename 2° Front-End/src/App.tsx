import { BrowserRouter, Route, Routes } from 'react-router-dom';
import { Shortcuts } from './components/Shortcuts';
import AccesoriosPage from './pages/AccesoriosPage';
import RealAdminPage from './pages/RealAdminPage';
import AnimePage from './pages/AnimePage';
import BlurayPage from './pages/BlurayPage';
import BlurrayPage from './pages/BlurrayPage';
import RealCarritoPage from './pages/RealCarritoPage';
import RealCheckoutPage from './pages/RealCheckoutPage';
import IndexPage from './pages/IndexPage';
import LoginPage from './pages/LoginPage';
import MerchandisingPage from './pages/MerchandisingPage';
import MusicaPage from './pages/MusicaPage';
import PeliculasPage from './pages/PeliculasPage';
import PerfilPage from './pages/PerfilPage';
import RegistroPage from './pages/RegistroPage';
import RealResultadosPage from './pages/RealResultadosPage';
import SeriesPage from './pages/SeriesPage';
import VideojuegosPage from './pages/VideojuegosPage';
import NotFoundPage from './pages/NotFoundPage';
import { ScrollToTop } from './components/ScrollToTop';
import { CartProvider } from './state/CartContext';
import { AuthProvider } from './state/AuthContext';
import { ProtectedRoute } from './components/ProtectedRoute';
import ProductoPage from './pages/ProductoPage';
import OfertasPage from './pages/OfertasPage';
import FavoritosPage from './pages/FavoritosPage';
import PedidosPage from './pages/PedidosPage';
import SoportePage from './pages/SoportePage';
import PostVentaPage from './pages/PostVentaPage';
import NotificacionesPage from './pages/NotificacionesPage';
import RecuperarCuentaPage from './pages/RecuperarCuentaPage';
import EliminarCuentaPage from './pages/EliminarCuentaPage';

export default function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
      <CartProvider>
      <ScrollToTop />
      <Shortcuts />
      <Routes>
        <Route path="/" element={<IndexPage />} />
      <Route path="/accesorios.html" element={<AccesoriosPage />} />
      <Route path="/admin.html" element={<ProtectedRoute admin><RealAdminPage /></ProtectedRoute>} />
      <Route path="/anime.html" element={<AnimePage />} />
      <Route path="/bluray.html" element={<BlurayPage />} />
      <Route path="/blurray.html" element={<BlurrayPage />} />
      <Route path="/carrito.html" element={<RealCarritoPage />} />
      <Route path="/checkout.html" element={<RealCheckoutPage />} />
      <Route path="/index.html" element={<IndexPage />} />
      <Route path="/login.html" element={<LoginPage />} />
      <Route path="/merchandising.html" element={<MerchandisingPage />} />
      <Route path="/musica.html" element={<MusicaPage />} />
      <Route path="/peliculas.html" element={<PeliculasPage />} />
      <Route path="/perfil.html" element={<ProtectedRoute><PerfilPage /></ProtectedRoute>} />
      <Route path="/registro.html" element={<RegistroPage />} />
      <Route path="/resultados.html" element={<RealResultadosPage />} />
      <Route path="/series.html" element={<SeriesPage />} />
      <Route path="/videojuegos.html" element={<VideojuegosPage />} />
        <Route path="/producto/:id" element={<ProductoPage />} />
        <Route path="/ofertas" element={<OfertasPage />} />
        <Route path="/favoritos" element={<ProtectedRoute><FavoritosPage /></ProtectedRoute>} />
        <Route path="/pedidos" element={<ProtectedRoute><PedidosPage /></ProtectedRoute>} />
        <Route path="/pedidos/:id" element={<ProtectedRoute><PedidosPage /></ProtectedRoute>} />
        <Route path="/soporte" element={<ProtectedRoute><SoportePage /></ProtectedRoute>} />
        <Route path="/soporte/:id" element={<ProtectedRoute><SoportePage /></ProtectedRoute>} />
        <Route path="/postventa" element={<ProtectedRoute><PostVentaPage /></ProtectedRoute>} />
        <Route path="/postventa/:id" element={<ProtectedRoute><PostVentaPage /></ProtectedRoute>} />
        <Route path="/notificaciones" element={<ProtectedRoute><NotificacionesPage /></ProtectedRoute>} />
        <Route path="/recuperar-cuenta" element={<RecuperarCuentaPage />} />
        <Route path="/eliminar-cuenta" element={<ProtectedRoute><EliminarCuentaPage /></ProtectedRoute>} />
        <Route path="*" element={<NotFoundPage />} />
      </Routes>
      </CartProvider>
      </AuthProvider>
    </BrowserRouter>
  );
}
