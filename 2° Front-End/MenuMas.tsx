import { useEffect, useRef, useState } from 'react';
import { Link, useLocation } from 'react-router-dom';
import { useAuth } from '../state/AuthContext';
import { storeApi } from '../services/storeApi';
import '../styles/menu-mas.css';

/** Navegación secundaria integrada a la barra principal, sin barra adicional. */
export function MenuMas() {
  const { session } = useAuth();
  const { pathname } = useLocation();
  const details = useRef<HTMLDetailsElement>(null);
  const [unread, setUnread] = useState<number | null>(null);

  useEffect(() => {
    if (!session) { setUnread(null); return; }
    let active = true;
    void storeApi.notifications.unread(session.token)
      .then(r => { if (active) setUnread(r.unreadCount); })
      .catch(() => { if (active) setUnread(null); });
    return () => { active = false; };
  }, [session?.token]);

  useEffect(() => { if (details.current) details.current.open = false; }, [pathname]);

  const item = (label: string, to: string) => (
    <Link className="nackson-menu-mas-link" to={to}
      onClick={() => { if (details.current) details.current.open = false; }}>
      {label}
    </Link>
  );

  return (
    <details ref={details} className="nackson-menu-mas">
      <summary aria-label="Más secciones de la tienda">Más <span aria-hidden="true">▾</span></summary>
      <div className="nackson-menu-mas-panel" aria-label="Secciones adicionales">
        {item('Ofertas', '/ofertas')}
        {item('Catálogo', '/resultados.html')}
        {session && <>
          {item('Mis pedidos', '/pedidos')}
          {item('Soporte', '/soporte')}
          {item('Postventa', '/postventa')}
          {item(unread === null ? 'Notificaciones' : `Notificaciones (${unread})`, '/notificaciones')}
          {item('Favoritos', '/favoritos')}
          {session.rol === 'ADMINISTRADOR' && item('Administración', '/admin.html')}
        </>}
      </div>
    </details>
  );
}
