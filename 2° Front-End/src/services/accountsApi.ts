/** DTO tomados de ms-accounts/src/main/java/.../dto (Lotes 5 y 6). */
import { http } from './http';
export type Rol = 'CLIENTE' | 'ADMINISTRADOR';
export interface RegistroRequest { rut: string; nombres: string; apellidos: string; correo: string; confirmacionCorreo: string; password: string; confirmacionPassword: string }
export interface LoginRequest { correo: string; password: string }
export interface LoginResponse { token: string; tipo: 'Bearer'; expiraEnSegundos: number; userId: number; nombres: string; rol: Rol }
export interface PerfilResponse { id: number; rut: string; nombres: string; apellidos: string; correo: string; rol: Rol }
export interface ActualizarPerfilRequest { nombres?: string; apellidos?: string; correo?: string }
export interface CambioPasswordRequest { passwordActual: string; nuevaPassword: string; confirmacionPassword: string }
export interface MensajeResponse { mensaje: string }
export const accountsApi = {
  register: (body: RegistroRequest) => http<PerfilResponse>('/api/v1/accounts/register', { method: 'POST', body }),
  login: (body: LoginRequest) => http<LoginResponse>('/api/v1/auth/login', { method: 'POST', body }),
  profile: (token: string) => http<PerfilResponse>('/api/v1/accounts/me', { token }),
  updateProfile: (token: string, body: ActualizarPerfilRequest) => http<PerfilResponse>('/api/v1/accounts/me', { method: 'PATCH', token, body }),
  password: (token: string, body: CambioPasswordRequest) => http<MensajeResponse>('/api/v1/accounts/me/password', { method: 'PUT', token, body }),
  logout: (token: string) => http<MensajeResponse>('/api/v1/auth/logout', { method: 'POST', token }),
};
