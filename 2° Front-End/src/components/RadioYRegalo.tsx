import { useCallback, useEffect, useRef, useState } from 'react';
import { Link } from 'react-router-dom';
import { storeApi, errorMessage, type PremioRuleta } from '../services/storeApi';
import { useAuth } from '../state/AuthContext';
import './radio-regalo.css';

type Estacion = { nombre: string; url?: string; embed?: string };
const ESTACIONES: Estacion[] = [
  { nombre: 'Lo-Fi & Chill Beats', url: 'https://stream.zeno.fm/f3wvbbqmdg8uv' },
  { nombre: 'Radio 2 — Países Bajos', url: 'https://icecast.omroep.nl/radio2-bb-mp3' },
  { nombre: 'Radio Concierto Chile', url: 'https://playerservices.streamtheworld.com/api/livestream-redirect/CONCIERTO.mp3' },
  // Reproductor embebido oficial ofrecido por Melo para Otaku.CL (Chile).
  // No es un MP3 directo: el oyente inicia la señal desde el reproductor incluido.
  { nombre: 'Otaku.CL — Anime (Chile)', embed: 'https://themelo.in/embed/24839' },
];

export function RadioYRegalo() {
  const { session } = useAuth();
  const audio = useRef<HTMLAudioElement>(null);
  const [radioAbierta, setRadioAbierta] = useState(false);
  const [estacion, setEstacion] = useState(0);
  const [volumen, setVolumen] = useState(65);
  const [reproduciendo, setReproduciendo] = useState(false);
  const [radioError, setRadioError] = useState('');
  const [regaloAbierto, setRegaloAbierto] = useState(false);
  const [estado, setEstado] = useState<PremioRuleta | null>(null);
  const [cargando, setCargando] = useState(false);
  const [errorOferta, setErrorOferta] = useState('');
  const [angulo, setAngulo] = useState(0);
  const [vueltas, setVueltas] = useState(0);

  const parar = useCallback(() => {
    audio.current?.pause();
    setReproduciendo(false);
  }, []);

  useEffect(() => {
    return () => { audio.current?.pause(); };
  }, []);

  useEffect(() => { if (audio.current) audio.current.volume = volumen / 100; }, [volumen]);

  const reproducir = async () => {
    const reproductor = audio.current;
    if (!reproductor) return;
    setRadioError('');
    try {
      await reproductor.play();
      setReproduciendo(true);
    } catch {
      setReproduciendo(false);
      setRadioError('No se pudo reproducir esta emisora. Prueba otra estación o revisa tu conexión.');
    }
  };

  const cambiarEstacion = (indice: number) => {
    const estabaReproduciendo = reproduciendo;
    parar();
    setEstacion(indice);
    setRadioError('');
    const reproductor = audio.current;
    if (reproductor) {
      const url = ESTACIONES[indice].url;
      if (!url) {
        reproductor.removeAttribute('src');
        reproductor.load();
        return;
      }
      reproductor.src = url;
      reproductor.load();
      if (estabaReproduciendo) {
        void reproductor.play().then(() => setReproduciendo(true)).catch(() => {
          setRadioError('La nueva emisora no está disponible en este navegador.');
        });
      }
    }
  };

  const abrirRegalo = async () => {
    setRegaloAbierto(true);
    setCargando(true);
    setErrorOferta('');
    setEstado(null);
    if (!session || session.rol !== 'CLIENTE') { setCargando(false); return; }
    try {
      const data = await storeApi.roulette.status(session.token);
      setEstado(data);
    } catch (e) {
      setErrorOferta(errorMessage(e));
    } finally { setCargando(false); }
  };

  const sortear = async () => {
    if (!session || session.rol !== 'CLIENTE' || cargando || !estado?.puedeGirar) return;
    setCargando(true);
    setErrorOferta('');
    try {
      const data = await storeApi.roulette.spin(session.token);
      setEstado(data);
      const indice = ['DESCUENTO_5', 'DESCUENTO_10', 'ENVIO_GRATIS', 'NINGUNO'].indexOf(data.premio);
      const proximaVuelta = vueltas + 1;
      setVueltas(proximaVuelta);
      setAngulo(proximaVuelta * 1800 - 45 - 90 * Math.max(0, indice));
      window.dispatchEvent(new Event('nackson-roulette-changed'));
    } catch (e) { setErrorOferta(errorMessage(e)); }
    finally { setCargando(false); }
  };

  const premioTexto = (p: string) => ({
    DESCUENTO_5: '5 % de descuento en tu próxima compra',
    DESCUENTO_10: '10 % de descuento en tu próxima compra',
    ENVIO_GRATIS: 'Despacho gratis en tu próxima compra a domicilio',
    NINGUNO: 'Esta vez no ganaste; ¡vuelve a intentarlo!'
  }[p] || 'Sin premio');

  return <>
    {session?.rol !== 'ADMINISTRADOR' && <div className="nr-regalo-widget">
      <button type="button" className="nr-fab nr-fab-gift" aria-label="Ver regalo y sorteo de ofertas" title="Descubre una oferta" onClick={() => void abrirRegalo()}>
        <i className="bi bi-gift-fill" aria-hidden="true" />
      </button>
    </div>}
    <div className="nr-radio-widget">
      <audio ref={audio} preload="none" src={ESTACIONES[0].url} onError={() => { setReproduciendo(false); setRadioError('No se pudo conectar con esta emisora.'); }} onEnded={() => setReproduciendo(false)} />
      {radioAbierta && <section className="nr-radio-panel" aria-label="Ñackson Radio Live">
        <div className="nr-panel-header"><strong>♫ ÑACKSON RADIO LIVE</strong><button type="button" aria-label="Cerrar radio" onClick={() => setRadioAbierta(false)}>×</button></div>
        <p className="nr-state">{reproduciendo ? '● Reproduciendo' : 'Radio en pausa'}</p>
        <label htmlFor="nr-select-radio">Estación</label>
        <select id="nr-select-radio" value={estacion} onChange={e => cambiarEstacion(Number(e.target.value))}>
          {ESTACIONES.map((e, i) => <option key={e.nombre} value={i}>{e.nombre}</option>)}
        </select>
        {!ESTACIONES[estacion].embed && <label>Volumen {volumen}% <input aria-label="Volumen de la radio" type="range" min="0" max="100" value={volumen} onChange={e => setVolumen(Number(e.target.value))}/></label>}
        {ESTACIONES[estacion].embed ? <>
          <iframe
            key={ESTACIONES[estacion].embed}
            src={ESTACIONES[estacion].embed}
            title="Otaku.CL — radio anime de Chile"
            loading="lazy"
            allow="autoplay"
            style={{ width: '100%', height: 110, border: 0, borderRadius: 8, backgroundColor: '#fff' }}
          />
          <p className="nr-state">Pulsa reproducir en el reproductor de Otaku.CL. Puedes consultar la <a href="https://otaku.cl/" target="_blank" rel="noopener noreferrer">radio original</a> si no carga.</p>
        </> :
          <button className="nr-radio-play" type="button" onClick={() => reproduciendo ? parar() : void reproducir()}>{reproduciendo ? '⏸ Pausar' : '▶ Reproducir'}</button>
        }
        {radioError && <p role="alert" className="nr-error">{radioError}</p>}
      </section>}
      <div className="nx-radio-bar"><div className="nx-radio-title"><i className="bi bi-soundwave"/><span><strong>Ñackson Radio Live</strong><small>{ESTACIONES[estacion].nombre}</small></span></div><div className="nx-radio-controls"><button aria-label="Emisora anterior" onClick={()=>cambiarEstacion((estacion+ESTACIONES.length-1)%ESTACIONES.length)}><i className="bi bi-skip-start-fill"/></button><button className="nx-radio-play" aria-label={reproduciendo?'Pausar':'Reproducir'} onClick={()=>ESTACIONES[estacion].embed?setRadioAbierta(true):reproduciendo?parar():void reproducir()}><i className={`bi bi-${reproduciendo?'pause':'play'}-fill`}/></button><button aria-label="Siguiente emisora" onClick={()=>cambiarEstacion((estacion+1)%ESTACIONES.length)}><i className="bi bi-skip-end-fill"/></button></div><span className="nx-radio-live">{radioError?'Señal no disponible':reproduciendo?'● EN VIVO':'En pausa'}</span><label className="nx-radio-volume"><i className="bi bi-volume-up"/><input aria-label="Volumen" type="range" min="0" max="100" value={volumen} disabled={!!ESTACIONES[estacion].embed} onChange={e=>setVolumen(Number(e.target.value))}/></label></div>
      <button type="button" className="nr-fab nr-fab-radio" aria-label={radioAbierta ? 'Ocultar radio' : 'Abrir radio'} title="Ñackson Radio" onClick={() => setRadioAbierta(!radioAbierta)}><i className="bi bi-music-note-beamed" aria-hidden="true" /></button>
    </div>
    {regaloAbierto && session?.rol !== 'ADMINISTRADOR' && <div className="nr-modal-backdrop" role="presentation" onMouseDown={e => { if (e.target === e.currentTarget) setRegaloAbierto(false); }}>
      <section className="nr-gift-modal" role="dialog" aria-modal="true" aria-label="Ruleta Ñackson Store">
        <button className="nr-modal-close" type="button" onClick={() => setRegaloAbierto(false)} aria-label="Cerrar">×</button>
        <h2>🎁 Ruleta Ñackson Store</h2>
        <p>Un giro cada 24 horas por usuario. Tu premio se guarda en tu cuenta y se aplicará automáticamente al validar el checkout.</p>
        {!session ? <p>Para participar debes <Link to="/login.html" onClick={() => setRegaloAbierto(false)}>iniciar sesión</Link>.</p> : <>
          <div className="nr-wheel-box" aria-hidden="true">
            <div className="nr-wheel-pointer" />
            <div className="nr-wheel" style={{ transform: `rotate(${angulo}deg)` }}>
              <span className="nr-s nr-s0">5 %</span>
              <span className="nr-s nr-s1">10 %</span>
              <span className="nr-s nr-s2">Envío gratis</span>
              <span className="nr-s nr-s3">Sin premio</span>
            </div>
          </div>
          {errorOferta && <p role="alert" className="nr-error">{errorOferta}</p>}
          <button className="nr-spin" type="button" disabled={cargando || !estado?.puedeGirar} onClick={() => void sortear()}>
            {cargando ? 'Consultando…' : estado?.puedeGirar ? '🎲 GIRAR AHORA' : 'Próximo giro pendiente'}
          </button>
          {estado && estado.estado !== 'SIN_GIRO' && <div className="nr-prize" role="status">
            <strong>{premioTexto(estado.premio)}</strong>
            {estado.estado === 'PENDIENTE' && <p>Premio disponible para tu próxima compra. Se aplica una sola vez.</p>}
            {estado.estado === 'RESERVADO' && <p>Premio reservado para el pedido #{estado.pedidoId}, pendiente de confirmación.</p>}
            {estado.estado === 'CONSUMIDO' && <p>Premio utilizado correctamente.</p>}
            {estado.proximoGiro && <p>Próximo giro desde: {new Date(estado.proximoGiro).toLocaleString('es-CL')}</p>}
          </div>}
        </>}
      </section>
    </div>}
  </>;
}
