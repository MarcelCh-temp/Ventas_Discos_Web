import { useEffect, useState, type FormEvent } from 'react';
import { useNavigate } from 'react-router-dom';
import { Link } from 'react-router-dom';
import { PageFrame } from '../components/PageFrame';
import { Navbar } from '../components/Navbar';
import { useAuth } from '../state/AuthContext';
import { accountsApi, type PerfilResponse, type CambioPasswordRequest } from '../services/accountsApi';
import { HttpFailure } from '../services/http';
import { validatePassword } from '../state/authValidation';
import css from '../styles/perfil.css?inline';

export default function PerfilPage() {
  const { session, forgetSession } = useAuth();
  const navigate = useNavigate();
  const [profile, setProfile] = useState<PerfilResponse | null>(null);
  const [nombres, setNombres] = useState('');
  const [apellidos, setApellidos] = useState('');
  const [correo, setCorreo] = useState('');
  const [message, setMessage] = useState('');
  const [passMessage, setPassMessage] = useState('');
  const [busy, setBusy] = useState(false);
  const [passBusy, setPassBusy] = useState(false);
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [passErrors, setPassErrors] = useState<Record<string, string>>({});
  const [passwords, setPasswords] = useState<CambioPasswordRequest>({ passwordActual: '', nuevaPassword: '', confirmacionPassword: '' });
  useEffect(() => {
    if (!session) return;
    let alive = true;
    accountsApi.profile(session.token).then(data => {
      if (!alive) return;
      setProfile(data); setNombres(data.nombres); setApellidos(data.apellidos); setCorreo(data.correo);
    }).catch(err => {
      if (!alive) return;
      if (err instanceof HttpFailure && err.status === 401) { forgetSession(); navigate('/login.html', { replace: true }); return; }
      setMessage(err instanceof Error ? err.message : 'No se pudo cargar el perfil.');
    });
    return () => { alive = false; };
  }, [session?.token, forgetSession, navigate]);
  async function saveProfile(e: FormEvent<HTMLFormElement>) {
    e.preventDefault(); setErrors({}); setMessage('');
    if (!session || !profile) return;
    const fields: Record<string, string> = {};
    if (!nombres.trim() || nombres.length > 80) fields.nombres = 'Nombres obligatorios, máximo 80 caracteres.';
    if (!apellidos.trim() || apellidos.length > 80) fields.apellidos = 'Apellidos obligatorios, máximo 80 caracteres.';
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(correo.trim()) || correo.length > 120) fields.correo = 'Ingresa un correo válido.';
    setErrors(fields); if (Object.keys(fields).length) return;
    const changes = {
      ...(nombres.trim() !== profile.nombres && { nombres: nombres.trim() }),
      ...(apellidos.trim() !== profile.apellidos && { apellidos: apellidos.trim() }),
      ...(correo.trim() !== profile.correo && { correo: correo.trim() }),
    };
    if (!Object.keys(changes).length) { setMessage('No hay cambios para guardar.'); return; }
    setBusy(true);
    try {
      const updated = await accountsApi.updateProfile(session.token, changes);
      setProfile(updated); setNombres(updated.nombres); setApellidos(updated.apellidos); setCorreo(updated.correo);
      setMessage('Perfil actualizado correctamente en el servidor.');
    } catch (err) {
      if (err instanceof HttpFailure) { setErrors(err.fields); setMessage(err.message); }
      else setMessage('No se pudo actualizar el perfil.');
    } finally { setBusy(false); }
  }
  async function savePassword(e: FormEvent<HTMLFormElement>) {
    e.preventDefault(); setPassMessage('');
    const issues = validatePassword(passwords); setPassErrors(issues);
    if (Object.keys(issues).length || !session) return;
    setPassBusy(true);
    try {
      const response = await accountsApi.password(session.token, passwords);
      setPasswords({ passwordActual: '', nuevaPassword: '', confirmacionPassword: '' });
      setPassMessage(response.mensaje || 'Contraseña actualizada en el servidor.');
    } catch (err) {
      if (err instanceof HttpFailure) { setPassErrors(err.fields); setPassMessage(err.message); }
      else setPassMessage('No se pudo actualizar la contraseña.');
    } finally { setPassBusy(false); }
  }
  return <PageFrame title="Mi Perfil y Seguridad - Ñackson Store" css={css} bodyClass="d-flex flex-column min-vh-100">
    <Navbar variant="account" />
    <section className="auth-section flex-grow-1">
      <video autoPlay muted loop playsInline className="hero-video-bg"><source src="/video/loginyregistro.mp4" type="video/mp4" /></video><div className="hero-overlay" />
      <div className="container position-relative z-2 py-4" style={{ maxWidth: 580 }}>
        <div className="auth-card p-4 p-md-5 text-light mb-4">
          <div className="text-center mb-4 border-bottom border-secondary pb-3"><h2 className="h3 text-white mb-1">Mi Perfil</h2><p className="text-light small font-monospace mb-0">Actualiza tus datos de cuenta.</p></div>
          {message && <div role="status" className="alert alert-info">{message}</div>}
          {!profile ? <p className="text-light" role="status">Cargando perfil desde Accounts…</p> : <form data-nackson-react-form="true" onSubmit={saveProfile} noValidate>
            <div className="text-center mb-4"><label className="form-label small fw-bold text-primary d-block mb-2">Avatar de la tienda</label>
              <img src="/img/logo.gif" alt="Avatar predeterminado" width="90" height="90" className="rounded-circle border border-danger object-fit-cover shadow-lg mb-2" />
              <div className="d-flex justify-content-center gap-3 flex-wrap mt-2" aria-label="Avatares de muestra (no guardados)">
                {['rock.jpg','chica2.jpg','cine.jpg','chicaanime.jpg','logo.gif'].map(file =>
                  <img key={file} src={`/img/${file}`} width="50" height="50" alt="Avatar de muestra" className="rounded-circle border border-secondary object-fit-cover avatar-option" />)}
              </div>
              <p className="small text-white-50 mt-3">Avatares de muestra: la API de Accounts todavía no permite guardarlos.</p>
            </div>
            <div className="mb-3"><label className="form-label fw-bold" htmlFor="nombrePerfil">Nombres</label>
              <input id="nombrePerfil" className="form-control bg-dark text-light border-secondary" value={nombres} onChange={e => setNombres(e.target.value)} />
              {errors.nombres && <small role="alert" className="text-warning">{errors.nombres}</small>}</div>
            <div className="mb-3"><label className="form-label fw-bold" htmlFor="apellidosPerfil">Apellidos</label>
              <input id="apellidosPerfil" className="form-control bg-dark text-light border-secondary" value={apellidos} onChange={e => setApellidos(e.target.value)} />
              {errors.apellidos && <small role="alert" className="text-warning">{errors.apellidos}</small>}</div>
            <div className="mb-3"><label className="form-label fw-bold" htmlFor="correoPerfil">Correo electrónico</label>
              <input id="correoPerfil" type="email" className="form-control bg-dark text-light border-secondary" value={correo} onChange={e => setCorreo(e.target.value)} />
              {errors.correo && <small role="alert" className="text-warning">{errors.correo}</small>}</div>
            <div className="mb-3"><span className="small text-white-50">RUT: {profile.rut} · Rol: {profile.rol} (no editables)</span></div>
            <button className="btn btn-neon w-100 py-2 rounded-pill" type="submit" disabled={busy}>{busy ? 'GUARDANDO…' : 'Guardar Cambios de Perfil'}</button>
          </form>}
        </div>
        <div className="auth-card p-4 p-md-4 text-light"><div className="mb-3 border-bottom border-secondary pb-2">
          <h3 className="h6 text-primary fw-bold mb-1"><i className="bi bi-shield-lock-fill me-2" />Seguridad y Contraseña</h3>
          <p className="text-white-50 small mb-0 font-monospace">Cambia tu contraseña de acceso.</p></div>
          {passMessage && <div role="status" className="alert alert-info">{passMessage}</div>}
          <form data-nackson-react-form="true" onSubmit={savePassword} noValidate>
            {([
              ['passwordActual', 'Contraseña Actual', 'current-password'],
              ['nuevaPassword', 'Nueva Contraseña (8 a 72 caracteres)', 'new-password'],
              ['confirmacionPassword', 'Confirmar Nueva Contraseña', 'new-password'],
            ] as const).map(([key,label,auto]) => <div className="mb-3" key={key}>
              <label htmlFor={key} className="form-label small fw-bold text-light">{label}</label>
              <input id={key} type="password" autoComplete={auto} className="form-control form-control-sm bg-dark text-light border-secondary"
                value={passwords[key]} onChange={e => setPasswords(v => ({ ...v, [key]: e.target.value }))} />
              {passErrors[key] && <small role="alert" className="text-warning">{passErrors[key]}</small>}
            </div>)}
            <button type="submit" className="btn btn-outline-primary w-100 py-2 rounded-pill fw-bold" disabled={passBusy}>{passBusy ? 'ACTUALIZANDO…' : 'Actualizar Contraseña'}</button>
          </form>
        </div>
        {session?.rol === "CLIENTE" && <p className="text-center mt-3"><Link className="text-warning" to="/eliminar-cuenta">Solicitar eliminación de cuenta</Link></p>}
      </div>
    </section>
  </PageFrame>;
}
