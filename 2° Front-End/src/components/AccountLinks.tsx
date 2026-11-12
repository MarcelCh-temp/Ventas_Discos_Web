import { Link, useNavigate } from 'react-router-dom';
import { useOptionalAuth } from '../state/AuthContext';
/** Controles de sesión; no cambia la estructura visual de las barras originales. */
export function AccountLinks() {
  const auth = useOptionalAuth();
  const session = auth?.session;
  const navigate = useNavigate();
  if (!session) return <>
    <Link to="/login.html" className="btn btn-outline-light btn-sm rounded-pill px-3"><i className="bi bi-person-fill me-1"/> INGRESAR</Link>
    <Link to="/registro.html" className="btn btn-primary btn-sm rounded-pill px-3"><i className="bi bi-person-plus-fill me-1"/> REGISTRO</Link>
  </>;
  return <>
    <Link to="/perfil.html" className="btn btn-outline-light btn-sm rounded-pill px-3"><i className="bi bi-person-circle me-1"/>{session.nombres}</Link>
    {session.rol === 'ADMINISTRADOR' && <Link to="/admin.html" className="btn btn-outline-warning btn-sm rounded-pill px-3">Admin</Link>}
    <button type="button" className="btn btn-outline-danger btn-sm rounded-pill" onClick={async () => {
      try { await auth?.signOut(); } catch { /* Gateway inalcanzable: la sesión local ya fue descartada. */ }
      navigate('/');
    }}>Salir</button>
  </>;
}
