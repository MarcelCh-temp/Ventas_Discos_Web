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
    const refresh = () => { void storeApi.notifications.unread(session.token)
      .then(r => { if (active) setUnread(r.unreadCount); })
      .catch(() => { if (active) setUnread(null); }); };
    refresh();
    const timer=window.setInterval(refresh,30000);
    window.addEventListener('nackson-notifications-changed',refresh);
    return () => { active = false; window.clearInterval(timer);window.removeEventListener('nackson-notifications-changed',refresh); };
  }, [session?.token, pathname]);

  useEffect(() => { if (details.current) details.current.open = false; }, [pathname]);
  useEffect(() => {
    const dismiss = (event: KeyboardEvent) => { if (event.key === 'Escape' && details.current) details.current.open=false; };
    const outside = (event: PointerEvent) => { if (details.current && event.target instanceof Node && !details.current.contains(event.target)) details.current.open=false; };
    window.addEventListener('keydown',dismiss);document.addEventListener('pointerdown',outside);
    return()=>{window.removeEventListener('keydown',dismiss);document.removeEventListener('pointerdown',outside);};
  },[]);

  const item = (label: string, to: string) => (
    <Link className="nackson-menu-mas-link" to={to}
      onClick={() => { if (details.current) details.current.open = false; }}>
      {label}
    </Link>
  );

  return (<>
    {session && <Link className="nackson-bell btn btn-outline-light btn-sm" to="/notificaciones" aria-label={`Notificaciones${unread === null ? '' : ` (${unread})`}`}><i className="bi bi-bell" aria-hidden="true"/>{unread !== null && unread > 0 && <span>{unread}</span>}</Link>}
    <details ref={details} className="nackson-menu-mas">
      <summary aria-label="Más secciones de la tienda"><i className="bi bi-list" aria-hidden="true"/> <span>Menú</span></summary>
      <div className="nackson-menu-mas-panel" aria-label="Secciones adicionales">
        <div className="nx-sidebar-heading"><strong>ÑACKSON STORE</strong><button type="button" onClick={()=>{if(details.current)details.current.open=false;}} aria-label="Cerrar menú">×</button></div>
        <span className="nx-sidebar-label">Explorar</span>
        {item('Inicio', '/')}
        {item('Ofertas', '/ofertas')}
        {item('Catálogo', '/resultados.html')}
        <span className="nx-sidebar-label">Categorías</span>
        {item('Música · CD / Vinilo','/musica.html')}
        {item('Películas','/peliculas.html')}
        {item('Series','/series.html')}
        {item('Anime','/anime.html')}
        {item('Videojuegos','/videojuegos.html')}
        {item('Blu-ray / DVD','/bluray.html')}
        {item('Accesorios','/accesorios.html')}
        {item('Merchandising','/merchandising.html')}
        {session&&<span className="nx-sidebar-label">Mi cuenta</span>}
        {session?.rol === 'CLIENTE' && <>
          {item('Mis pedidos', '/pedidos')}
          {item('Soporte', '/soporte')}
          {item('Postventa', '/postventa')}
          {item(unread === null ? 'Notificaciones' : `Notificaciones (${unread})`, '/notificaciones')}
          {item('Favoritos', '/favoritos')}
        </>}
        {session?.rol === 'ADMINISTRADOR' && item('Administración', '/admin.html')}
      </div>
    </details>
  </>);
}
