import { Navigate, useLocation } from 'react-router-dom';
import { useAuth } from '../state/AuthContext';
import type { ReactNode } from 'react';
export function ProtectedRoute({ children, admin = false }: { children: ReactNode; admin?: boolean }) {
  const { session } = useAuth();
  const location = useLocation();
  if (!session) return <Navigate to="/login.html" replace state={{ from: location.pathname }} />;
  if (admin && session.rol !== 'ADMINISTRADOR') return <Navigate to="/" replace />;
  return <>{children}</>;
}
