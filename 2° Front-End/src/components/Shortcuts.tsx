import { Link, useLocation } from 'react-router-dom';
import { useEffect, useState } from 'react';
import { storeApi } from '../services/storeApi';
import { useAuth } from '../state/AuthContext';

export function Shortcuts() {
  const { session } = useAuth();
  const { pathname } = useLocation();
  const [unread, setUnread] = useState<number | null>(null);

  useEffect(() => {
    if (!session) {
      setUnread(null);
      return;
    }

    let active = true;
    void storeApi.notifications.unread(session.token)
      .then(response => {
        if (active) setUnread(response.unreadCount);
      })
      .catch(() => {
        if (active) setUnread(null);
      });

    return () => { active = false; };
  }, [session?.token]);

  // Conserva la corrección anterior: sin accesos superiores en login ni registro.
  if (pathname === '/login.html' || pathname === '/registro.html') {
    return null;
  }

  // La franja de colores se aplica solamente a Mi Perfil.
  const esPerfil = pathname === '/perfil.html';

  return (
    <nav
      aria-label="Secciones de la tienda"
      className={esPerfil
        ? 'shortcuts-perfil'
        : 'bg-black border-bottom border-secondary py-1'}
    >
      <div className={esPerfil
        ? "container d-flex align-items-center justify-content-center flex-wrap gap-3 small"
        : "container d-flex align-items-center flex-wrap gap-3 small"}>
        <Link className="text-info text-decoration-none" to="/ofertas">Ofertas</Link>
        <Link className="text-info text-decoration-none" to="/resultados.html">Catálogo</Link>

        {session?.rol === 'CLIENTE' && (
          <>
            <Link className="text-info text-decoration-none" to="/pedidos">Mis pedidos</Link>
            <Link className="text-info text-decoration-none" to="/soporte">Soporte</Link>
            <Link className="text-info text-decoration-none" to="/postventa">Postventa</Link>
            <Link className="text-info text-decoration-none" to="/notificaciones">
              {unread === null ? 'Notificaciones' : `Notificaciones (${unread})`}
            </Link>
            <Link className="text-info text-decoration-none" to="/favoritos">Favoritos</Link>


          </>
        )}
        {session?.rol === 'ADMINISTRADOR' && <Link className="text-warning text-decoration-none" to="/admin.html">Administración</Link>}
      </div>
    </nav>
  );
}
