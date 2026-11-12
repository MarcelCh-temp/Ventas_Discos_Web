/** Cliente REST compartido: todas las llamadas pasan por el Gateway. */
import type { ApiError } from '../models/ApiError';

const apiBase = (import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080').replace(/\/$/, '');

export class HttpFailure extends Error {
  constructor(public readonly status: number, message: string, public readonly fields: Record<string, string> = {}) {
    super(message);
    this.name = 'HttpFailure';
  }
}

export async function http<T>(path: string, options: {
  method?: 'GET' | 'POST' | 'PATCH' | 'PUT' | 'DELETE';
  body?: unknown;
  token?: string;
} = {}): Promise<T> {
  const headers: Record<string, string> = { Accept: 'application/json' };
  if (options.body !== undefined) headers['Content-Type'] = 'application/json';
  if (options.token) headers.Authorization = `Bearer ${options.token}`;
  let response: Response;
  try {
    response = await fetch(`${apiBase}${path}`, {
      method: options.method || 'GET', headers,
      body: options.body === undefined ? undefined : JSON.stringify(options.body),
      cache: 'no-store',
    });
  } catch {
    throw new HttpFailure(0, 'No se pudo conectar con el API Gateway (localhost:8080). Comprueba que esté encendido.');
  }
  const raw = await response.text();
  let payload: unknown;
  try { payload = raw ? JSON.parse(raw) : undefined; } catch { payload = undefined; }
  if (!response.ok) {
    const error = payload && typeof payload === 'object' ? payload as Partial<ApiError> : {};
    throw new HttpFailure(response.status, error.mensaje || `El servidor respondió con error ${response.status}.`,
      error.errores && typeof error.errores === 'object' ? error.errores : {});
  }
  return payload as T;
}
