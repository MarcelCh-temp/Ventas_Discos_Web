import type { RegistroRequest, LoginRequest, CambioPasswordRequest } from '../services/accountsApi';
export type FieldErrors = Record<string, string>;
const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
export function validarRut(value: string): boolean {
  const norm = value.replace(/[.\-\s]/g, '').toUpperCase();
  if (!/^\d{7,8}[0-9K]$/.test(norm)) return false;
  let sum = 0; let factor = 2;
  for (let i = norm.length - 2; i >= 0; i--) { sum += Number(norm[i]) * factor; factor = factor === 7 ? 2 : factor + 1; }
  const rest = 11 - sum % 11;
  return norm.at(-1) === (rest === 11 ? '0' : rest === 10 ? 'K' : String(rest));
}
export function validateLogin(input: LoginRequest): FieldErrors {
  const errs: FieldErrors = {};
  if (!emailPattern.test(input.correo.trim())) errs.correo = 'Ingresa un correo válido.';
  if (!input.password) errs.password = 'La contraseña es obligatoria.';
  return errs;
}
export function validateRegistration(input: RegistroRequest): FieldErrors {
  const errs: FieldErrors = {};
  if (!validarRut(input.rut)) errs.rut = 'Ingresa un RUT chileno válido con dígito verificador.';
  if (!input.nombres.trim() || input.nombres.trim().length > 80) errs.nombres = 'Ingresa nombres (máximo 80 caracteres).';
  if (!input.apellidos.trim() || input.apellidos.trim().length > 80) errs.apellidos = 'Ingresa apellidos (máximo 80 caracteres).';
  if (!emailPattern.test(input.correo.trim()) || input.correo.length > 120) errs.correo = 'Ingresa un correo válido (máximo 120 caracteres).';
  if (!input.confirmacionCorreo.trim() || input.correo.trim().toLowerCase() !== input.confirmacionCorreo.trim().toLowerCase()) errs.confirmacionCorreo = 'Los correos no coinciden.';
  if (input.password.length < 8 || input.password.length > 72) errs.password = 'La contraseña debe tener entre 8 y 72 caracteres.';
  if (!input.confirmacionPassword || input.password !== input.confirmacionPassword) errs.confirmacionPassword = 'Las contraseñas no coinciden.';
  return errs;
}
export function validatePassword(input: CambioPasswordRequest): FieldErrors {
  const errs: FieldErrors = {};
  if (!input.passwordActual) errs.passwordActual = 'Ingresa tu contraseña actual.';
  if (input.nuevaPassword.length < 8 || input.nuevaPassword.length > 72) errs.nuevaPassword = 'Usa entre 8 y 72 caracteres.';
  if (!input.confirmacionPassword || input.nuevaPassword !== input.confirmacionPassword) errs.confirmacionPassword = 'Las contraseñas no coinciden.';
  return errs;
}
