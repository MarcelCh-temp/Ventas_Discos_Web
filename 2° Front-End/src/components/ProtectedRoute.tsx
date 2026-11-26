import { useEffect, useState, type ReactNode } from 'react';
import { Navigate, useLocation } from 'react-router-dom';
import { useAuth } from '../state/AuthContext';
import { accountsApi } from '../services/accountsApi';

type AdminCheck = {
  token: string;
  status: 'checking' | 'allowed' | 'denied' | 'unavailable';
};

/**
 * Proteccion visual de rutas de clientes y administración.
 * El rol almacenado localmente NO es fuente de autoridad:
 * para /admin.html se comprueba de nuevo con /api/v1/accounts/me.
 * El Gateway y los microservicios siguen siendo los responsables
 * de validar el JWT y el rol en CADA solicitud administrativa.
 */
export function ProtectedRoute({ children, admin = false, shared = false, guest = false }: { children: ReactNode; admin?: boolean; shared?: boolean; guest?: boolean }) {
  const { session, forgetSession } = useAuth();
  const location = useLocation();
  const token = session?.token ?? '';
  const [adminCheck, setAdminCheck] = useState<AdminCheck>({ token: '', status: 'checking' });

  useEffect(() => {
    if (!session || (admin && session.rol !== 'ADMINISTRADOR') || (!admin && !shared && session.rol !== 'CLIENTE')) return;
    let active = true;
    const verifyingToken = session.token;

    // No se muestra el panel mientras no exista verificacion vigente para ESTE token.
    void accountsApi.profile(verifyingToken)
      .then(profile => {
        if (!active) return;
        setAdminCheck({
          token: verifyingToken,
          status: profile.id === session.userId && profile.rol === session.rol
            ? 'allowed'
            : 'denied',
        });
      })
      .catch(error => {
        if (!active) return;
        const code = typeof error === 'object' && error !== null && 'status' in error
          ? (error as { status?: unknown }).status
          : undefined;
        // Un error de red no se interpreta como permiso de administración.
        setAdminCheck({
          token: verifyingToken,
          status: code === 401 || code === 403 ? 'denied' : 'unavailable',
        });
      });

    return () => { active = false; };
  }, [admin, shared, session?.token, session?.userId, session?.rol]);

  if (!session && guest) return <>{children}</>;
  if (!session) {
    return <Navigate to="/login.html" replace state={{ from: location.pathname + location.search }} />;
  }
  if (!admin) {
    if (!shared && session.rol !== 'CLIENTE') return <Navigate to="/admin.html" replace />;
  }
  if (admin && session.rol !== 'ADMINISTRADOR') return <Navigate to="/" replace />;

  // Compara el token para no reutilizar una autorización de otra sesión.
  if (adminCheck.token !== token || adminCheck.status === 'checking') {
    return <main className="container py-5" role="status">Verificando acceso…</main>;
  }
  if (adminCheck.status === 'denied') {
    return <AccessDenied onExit={forgetSession} />;
  }
  if (adminCheck.status === 'unavailable') {
    return <main className="container py-5" role="alert">
      No se pudo verificar tu sesión y permisos. Comprueba que MS-Accounts y API Gateway estén encendidos y recarga la página.
    </main>;
  }
  return <>{children}</>;
}

function AccessDenied({ onExit }: { onExit: () => void }) {
  useEffect(() => { onExit(); }, [onExit]);
  return <Navigate to="/login.html" replace />;
}
