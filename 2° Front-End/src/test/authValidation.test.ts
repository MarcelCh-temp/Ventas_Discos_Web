import { describe, expect, it } from 'vitest';
import { validarRut, validateLogin, validateRegistration, validatePassword } from '../state/authValidation';
import type { RegistroRequest } from '../services/accountsApi';
const valid: RegistroRequest = {
  rut:'12.345.678-5', nombres:'Ana', apellidos:'Pérez', correo:'ana@example.cl', confirmacionCorreo:'ANA@example.cl',
  password:'ClaveSegura123', confirmacionPassword:'ClaveSegura123',
};
describe('Validaciones de cuentas (mismo formato que Accounts)', () => {
  it('acepta RUT correcto y normalizado', () => expect(validarRut('12345678-5')).toBe(true));
  it('rechaza DV incorrecto', () => expect(validarRut('12345678-9')).toBe(false));
  it('rechaza RUT corto', () => expect(validarRut('123-4')).toBe(false));
  it('acepta registro correcto', () => expect(validateRegistration(valid)).toEqual({}));
  it('rechaza RUT inválido', () => expect(validateRegistration({...valid, rut:'11111111-2'}).rut).toBeDefined());
  it('requiere nombres', () => expect(validateRegistration({...valid, nombres:' '}).nombres).toBeDefined());
  it('requiere apellidos', () => expect(validateRegistration({...valid, apellidos:' '}).apellidos).toBeDefined());
  it('rechaza correo mal escrito', () => expect(validateRegistration({...valid, correo:'incorrecto'}).correo).toBeDefined());
  it('rechaza confirmación de correo distinta', () => expect(validateRegistration({...valid, confirmacionCorreo:'otra@ejemplo.cl'}).confirmacionCorreo).toBeDefined());
  it('rechaza confirmación de correo vacía', () => expect(validateRegistration({...valid, confirmacionCorreo:''}).confirmacionCorreo).toBeDefined());
  it('requiere contraseña de ocho caracteres', () => expect(validateRegistration({...valid, password:'1234567'}).password).toBeDefined());
  it('rechaza confirmación de contraseña distinta', () => expect(validateRegistration({...valid, confirmacionPassword:'distinta'}).confirmacionPassword).toBeDefined());
  it('valida login', () => expect(validateLogin({correo:'ana@example.cl', password:'hola'})).toEqual({}));
  it('rechaza login sin contraseña', () => expect(validateLogin({correo:'ana@example.cl',password:''}).password).toBeDefined());
  it('rechaza login con correo inválido', () => expect(validateLogin({correo:'ana',password:'123'}).correo).toBeDefined());
  it('rechaza contraseña nueva demasiado corta', () => expect(validatePassword({passwordActual:'xx',nuevaPassword:'123',confirmacionPassword:'123'}).nuevaPassword).toBeDefined());
  it('rechaza confirmación de contraseña nueva distinta', () => expect(validatePassword({passwordActual:'xx',nuevaPassword:'12345678',confirmacionPassword:'zzzzzzzz'}).confirmacionPassword).toBeDefined());
});
