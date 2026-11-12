import { afterEach, describe, expect, it, vi } from 'vitest';
import { accountsApi } from '../services/accountsApi';
import { HttpFailure } from '../services/http';
const success = (data: unknown) => ({ ok:true, status:200, text: async () => JSON.stringify(data) });
afterEach(() => vi.unstubAllGlobals());
describe('Contratos REST de Accounts: rutas por Gateway', () => {
  it('login usa POST /auth/login, correo y contraseña sin userId', async () => {
    const fetchMock = vi.fn().mockResolvedValue(success({token:'jwt',tipo:'Bearer',expiraEnSegundos:100,userId:1,nombres:'Ana',rol:'CLIENTE'}));
    vi.stubGlobal('fetch', fetchMock);
    await accountsApi.login({correo:'ana@example.cl',password:'secreto'});
    const [url, config] = fetchMock.mock.calls[0];
    expect(url).toBe('http://localhost:8080/api/v1/auth/login');
    expect(config.method).toBe('POST');
    expect(JSON.parse(config.body)).toEqual({correo:'ana@example.cl',password:'secreto'});
  });
  it('registro usa ruta pública y no manda rol', async () => {
    const f = vi.fn().mockResolvedValue(success({id:1})); vi.stubGlobal('fetch', f);
    await accountsApi.register({rut:'12.345.678-5',nombres:'Ana',apellidos:'Pérez',correo:'ana@example.cl',confirmacionCorreo:'ana@example.cl',password:'12345678',confirmacionPassword:'12345678'});
    expect(f.mock.calls[0][0]).toContain('/api/v1/accounts/register');
    expect(JSON.parse(f.mock.calls[0][1].body)).not.toHaveProperty('rol');
  });
  it('perfil agrega token Bearer', async () => {
    const f = vi.fn().mockResolvedValue(success({id:1})); vi.stubGlobal('fetch', f);
    await accountsApi.profile('TOKEN');
    expect(f.mock.calls[0][1].headers.Authorization).toBe('Bearer TOKEN');
    expect(f.mock.calls[0][0]).toContain('/api/v1/accounts/me');
  });
  it('cambio contraseña utiliza PUT y los campos exactos', async () => {
    const f = vi.fn().mockResolvedValue(success({mensaje:'ok'})); vi.stubGlobal('fetch', f);
    await accountsApi.password('TOKEN', {passwordActual:'a',nuevaPassword:'12345678',confirmacionPassword:'12345678'});
    expect(f.mock.calls[0][1].method).toBe('PUT');
    expect(f.mock.calls[0][0]).toContain('/me/password');
  });
  it('propaga errores 400 por campo del backend', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(({ok:false,status:400,text:async () => JSON.stringify({status:400,mensaje:'Dato inválido',errores:{correo:'Duplicado'}})})));
    await expect(accountsApi.login({correo:'x@y.cl',password:'x'})).rejects.toMatchObject({status:400,fields:{correo:'Duplicado'}});
  });
  it('informa si Gateway no responde, sin fingir éxito', async () => {
    vi.stubGlobal('fetch', vi.fn().mockRejectedValue(new Error('failed')));
    await expect(accountsApi.login({correo:'x@y.cl',password:'x'})).rejects.toBeInstanceOf(HttpFailure);
  });
});
