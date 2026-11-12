import { useState, type FormEvent } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { PageFrame } from '../components/PageFrame';
import { Navbar } from '../components/Navbar';
import { useAuth } from '../state/AuthContext';
import { validateLogin } from '../state/authValidation';
import { HttpFailure } from '../services/http';
import css from '../styles/login.css?inline';

export default function LoginPage() {
  const { signIn, session } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [correo, setCorreo] = useState('');
  const [password, setPassword] = useState('');
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [message, setMessage] = useState('');
  const [busy, setBusy] = useState(false);
  const previous = (location.state as { from?: unknown; registered?: boolean } | null);
  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setMessage('');
    const input = { correo: correo.trim(), password };
    const issues = validateLogin(input);
    setErrors(issues);
    if (Object.keys(issues).length) return;
    setBusy(true);
    try {
      const user = await signIn(input);
      const from = previous?.from;
      const target = typeof from === 'string' && from.startsWith('/') && !from.startsWith('//')
        ? from : user.rol === 'ADMINISTRADOR' ? '/admin.html' : '/perfil.html';
      navigate(target, { replace: true });
    } catch (err) {
      if (err instanceof HttpFailure) { setErrors(err.fields); setMessage(err.message); }
      else setMessage(err instanceof Error ? err.message : 'No se pudo iniciar sesión.');
    } finally { setBusy(false); }
  }
  return <PageFrame title="Iniciar Sesión - Ñackson Store" css={css} bodyClass="d-flex flex-column min-vh-100">
    <Navbar variant="account" />
    <section className="auth-section flex-grow-1">
      <video autoPlay muted loop playsInline className="hero-video-bg"><source src="/video/loginyregistro.mp4" type="video/mp4" /></video>
      <div className="hero-overlay" />
      <div className="container position-relative z-2 py-4" style={{ maxWidth: 480 }}>
        <div className="auth-card p-4 p-md-5 text-light">
          <div className="text-center mb-4 border-bottom border-secondary pb-3">
            <h2 className="h3 text-white mb-1">Iniciar Sesión</h2>
            <p className="text-light small font-monospace mb-0">Accede a tu cuenta de Ñackson Store</p>
          </div>
          {previous?.registered && <div role="status" className="alert alert-success">Cuenta creada correctamente. Inicia sesión con tus credenciales.</div>}
          {session && <div role="status" className="alert alert-info">Ya tienes una sesión activa. <Link to="/perfil.html">Ir a mi perfil</Link></div>}
          {message && <div role="alert" className="alert alert-danger">{message}</div>}
          <form data-nackson-react-form="true" onSubmit={submit} noValidate>
            <div className="mb-3 text-start">
              <label htmlFor="emailLogin" className="form-label small fw-bold text-light">Correo Electrónico</label>
              <div className="input-group"><span className="input-group-text bg-dark border-secondary text-primary"><i className="bi bi-envelope-fill" /></span>
                <input autoComplete="email" type="email" className="form-control bg-dark text-light border-secondary" id="emailLogin" value={correo} onChange={e => setCorreo(e.target.value)} placeholder="nombre@correo.com" aria-invalid={!!errors.correo} />
              </div>{errors.correo && <small role="alert" className="text-warning">{errors.correo}</small>}
            </div>
            <div className="mb-4 text-start"><label htmlFor="passwordLogin" className="form-label small fw-bold text-light">Contraseña</label>
              <div className="input-group"><span className="input-group-text bg-dark border-secondary text-primary"><i className="bi bi-shield-lock-fill" /></span>
                <input type="password" autoComplete="current-password" className="form-control bg-dark text-light border-secondary" id="passwordLogin" value={password} onChange={e => setPassword(e.target.value)} placeholder="••••••••" aria-invalid={!!errors.password} />
              </div>{errors.password && <small role="alert" className="text-warning">{errors.password}</small>}
            </div>
            <button type="submit" className="btn btn-neon w-100 py-2 rounded-pill mb-3" disabled={busy}>{busy ? 'CONECTANDO…' : 'INGRESAR'}</button>
            <div className="text-center mb-2"><Link to="/recuperar-cuenta" className="text-info small">¿Olvidaste tu correo o contraseña?</Link></div>
            <div className="text-center"><Link to="/registro.html" className="text-decoration-none small text-primary hover-light font-monospace">¿No tienes una cuenta? Regístrate aquí</Link></div>
          </form>
        </div>
      </div>
    </section>
  </PageFrame>;
}
