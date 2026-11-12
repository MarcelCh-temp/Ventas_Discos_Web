import { useState, type FormEvent } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { PageFrame } from '../components/PageFrame';
import { Navbar } from '../components/Navbar';
import { accountsApi, type RegistroRequest } from '../services/accountsApi';
import { HttpFailure } from '../services/http';
import { validateRegistration } from '../state/authValidation';
import css from '../styles/registro.css?inline';

const blank: RegistroRequest = { rut: '', nombres: '', apellidos: '', correo: '', confirmacionCorreo: '', password: '', confirmacionPassword: '' };
const fields: { key: keyof RegistroRequest; label: string; type: string; placeholder: string; autoComplete?: string }[] = [
  { key: 'rut', label: 'RUT', type: 'text', placeholder: '12.345.678-5' },
  { key: 'nombres', label: 'Nombres', type: 'text', placeholder: 'Tus nombres', autoComplete: 'given-name' },
  { key: 'apellidos', label: 'Apellidos', type: 'text', placeholder: 'Tus apellidos', autoComplete: 'family-name' },
  { key: 'correo', label: 'Correo electrónico', type: 'email', placeholder: 'tucorreo@ejemplo.cl', autoComplete: 'email' },
  { key: 'confirmacionCorreo', label: 'Confirmar correo', type: 'email', placeholder: 'Repite tu correo' },
  { key: 'password', label: 'Contraseña (8 a 72 caracteres)', type: 'password', placeholder: '••••••••', autoComplete: 'new-password' },
  { key: 'confirmacionPassword', label: 'Confirmar contraseña', type: 'password', placeholder: 'Repite tu contraseña', autoComplete: 'new-password' },
];
export default function RegistroPage() {
  const [value, setValue] = useState<RegistroRequest>(blank);
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [message, setMessage] = useState('');
  const [busy, setBusy] = useState(false);
  const navigate = useNavigate();
  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault(); setMessage('');
    const issues = validateRegistration(value);
    setErrors(issues); if (Object.keys(issues).length) return;
    setBusy(true);
    try {
      await accountsApi.register({ ...value, rut: value.rut.trim(), nombres: value.nombres.trim(), apellidos: value.apellidos.trim(), correo: value.correo.trim(), confirmacionCorreo: value.confirmacionCorreo.trim() });
      navigate('/login.html', { replace: true, state: { registered: true } });
    } catch (err) {
      if (err instanceof HttpFailure) { setErrors(err.fields); setMessage(err.message); }
      else setMessage('No se pudo registrar la cuenta.');
    } finally { setBusy(false); }
  }
  return <PageFrame title="Registro - Ñackson Store" css={css} bodyClass="d-flex flex-column min-vh-100">
    <Navbar variant="register" />
    <section className="auth-section flex-grow-1">
      <video autoPlay muted loop playsInline className="hero-video-bg"><source src="/video/fondo.mp4" type="video/mp4" /></video>
      <div className="hero-overlay" />
      <div className="container position-relative z-2" style={{ maxWidth: 450 }}>
        <div className="auth-card p-4 p-md-5 text-light">
          <div className="text-center mb-4"><h2 className="h3 text-white mb-2">Crear Cuenta</h2><p className="text-white-50 small font-monospace">Únete a la comunidad de Ñackson Store</p></div>
          {message && <div role="alert" className="alert alert-danger">{message}</div>}
          <form data-nackson-react-form="true" onSubmit={submit} noValidate>
            {fields.map(field => <div className="mb-3" key={field.key}>
              <label htmlFor={`reg-${field.key}`} className="form-label small fw-bold">{field.label}</label>
              <input id={`reg-${field.key}`} type={field.type} autoComplete={field.autoComplete}
                className="form-control bg-dark text-light border-secondary"
                placeholder={field.placeholder} value={value[field.key]} aria-invalid={!!errors[field.key]}
                onChange={e => setValue(v => ({ ...v, [field.key]: e.target.value }))} />
              {errors[field.key] && <small role="alert" className="text-warning">{errors[field.key]}</small>}
            </div>)}
            <button type="submit" className="btn btn-neon w-100 py-2 mb-3 rounded-pill" disabled={busy}>{busy ? 'REGISTRANDO…' : 'REGISTRARSE'}</button>
            <div className="text-center"><p className="small text-white-50 mb-0">¿Ya tienes una cuenta? <Link to="/login.html" className="text-primary text-decoration-none fw-bold hover-light">Ingresa aquí</Link></p></div>
          </form>
        </div>
      </div>
    </section>
  </PageFrame>;
}
