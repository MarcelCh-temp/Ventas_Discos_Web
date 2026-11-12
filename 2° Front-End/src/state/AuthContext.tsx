import { createContext, useCallback, useContext, useMemo, useState, type ReactNode } from 'react';
import { accountsApi, type LoginRequest, type LoginResponse, type Rol } from '../services/accountsApi';

export interface AuthSession { token: string; userId: number; nombres: string; rol: Rol; expiresAt: number }
const KEY = 'nackson:auth:v1';
function readSession(): AuthSession | null {
  try {
    const raw = sessionStorage.getItem(KEY);
    if (!raw) return null;
    const data: unknown = JSON.parse(raw);
    if (!data || typeof data !== 'object') return null;
    const value = data as Partial<AuthSession>;
    if (!value.token || typeof value.token !== 'string' || !Number.isFinite(value.expiresAt) ||
      Number(value.expiresAt) <= Date.now() || !Number.isSafeInteger(value.userId) ||
      !['CLIENTE', 'ADMINISTRADOR'].includes(value.rol || '')) {
      sessionStorage.removeItem(KEY); return null;
    }
    return value as AuthSession;
  } catch { return null; }
}
interface AuthValue {
  session: AuthSession | null;
  signIn: (input: LoginRequest) => Promise<AuthSession>;
  signOut: () => Promise<void>;
  forgetSession: () => void;
}
const AuthContext = createContext<AuthValue | null>(null);
export function AuthProvider({ children }: { children: ReactNode }) {
  const [session, setSession] = useState<AuthSession | null>(readSession);
  const forgetSession = useCallback(() => { sessionStorage.removeItem(KEY); setSession(null); }, []);
  const signIn = useCallback(async (input: LoginRequest) => {
    const response: LoginResponse = await accountsApi.login(input);
    if (!response.token || response.tipo !== 'Bearer' || !Number.isFinite(response.expiraEnSegundos) ||
      response.expiraEnSegundos <= 0 || !Number.isSafeInteger(response.userId) ||
      !['CLIENTE','ADMINISTRADOR'].includes(response.rol)) {
      throw new Error('La respuesta de autenticación del servidor es inválida.');
    }
    const next: AuthSession = { token: response.token, userId: response.userId,
      nombres: response.nombres, rol: response.rol,
      expiresAt: Date.now() + response.expiraEnSegundos * 1000 };
    sessionStorage.setItem(KEY, JSON.stringify(next)); setSession(next); return next;
  }, []);
  const signOut = useCallback(async () => {
    // El logout JWT es sin estado: siempre se descarta la copia local, incluso si Gateway no responde.
    const current = readSession();
    try { if (current) await accountsApi.logout(current.token); }
    finally { forgetSession(); }
  }, [forgetSession]);
  const active = session && session.expiresAt > Date.now() ? session : null;
  const value = useMemo(() => ({ session: active, signIn, signOut, forgetSession }), [active, signIn, signOut, forgetSession]);
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}
export function useAuth() {
  const value = useContext(AuthContext);
  if (!value) throw new Error('useAuth debe utilizarse dentro de AuthProvider');
  return value;
}

/** Uso opcional para componentes compartidos por pruebas de la Etapa 4. */
export function useOptionalAuth() { return useContext(AuthContext); }
