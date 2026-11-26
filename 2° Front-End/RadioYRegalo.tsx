import { useCallback, useEffect, useRef, useState } from 'react';
import { Link } from 'react-router-dom';
import { storeApi, type Oferta } from '../services/storeApi';
import './radio-regalo.css';

const ESTACIONES = [
  { nombre: 'Lo-Fi & Chill Beats', url: 'https://stream.zeno.fm/f3wvbbqmdg8uv' },
  { nombre: 'Radio 2 — Países Bajos', url: 'https://icecast.omroep.nl/radio2-bb-mp3' },
  { nombre: 'Radio Concierto Chile', url: 'https://playerservices.streamtheworld.com/api/livestream-redirect/CONCIERTO.mp3' },
] as const;

export function RadioYRegalo() {
  const audio = useRef<HTMLAudioElement>(null);
  const [radioAbierta, setRadioAbierta] = useState(false);
  const [estacion, setEstacion] = useState(0);
  const [reproduciendo, setReproduciendo] = useState(false);
  const [radioError, setRadioError] = useState('');
  const [regaloAbierto, setRegaloAbierto] = useState(false);
  const [ofertas, setOfertas] = useState<Oferta[]>([]);
  const [cargando, setCargando] = useState(false);
  const [errorOferta, setErrorOferta] = useState('');
  const [premio, setPremio] = useState<Oferta | null>(null);

  const parar = useCallback(() => {
    audio.current?.pause();
    setReproduciendo(false);
  }, []);

  useEffect(() => {
    return () => { audio.current?.pause(); };
  }, []);

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
      reproductor.src = ESTACIONES[indice].url;
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
    setPremio(null);
    setCargando(true);
    setErrorOferta('');
    try {
      const response = await storeApi.catalog.offers();
      setOfertas(response.filter(o => Array.isArray(o.productos) && o.productos.length > 0));
    } catch {
      setOfertas([]);
      setErrorOferta('No se pudieron consultar las ofertas del servidor. Vuelve a intentarlo.');
    } finally {
      setCargando(false);
    }
  };

  const sortear = () => {
    if (!ofertas.length) return;
    const seleccion = ofertas[Math.floor(Math.random() * ofertas.length)];
    setPremio(seleccion);
  };

  return <>
    <div className="nr-regalo-widget">
      <button type="button" className="nr-fab nr-fab-gift" aria-label="Ver regalo y sorteo de ofertas" title="Descubre una oferta" onClick={() => void abrirRegalo()}>
        <i className="bi bi-gift-fill" aria-hidden="true" />
      </button>
    </div>
    <div className="nr-radio-widget">
      {radioAbierta && <section className="nr-radio-panel" aria-label="Ñackson Radio Live">
        <div className="nr-panel-header"><strong>♫ ÑACKSON RADIO LIVE</strong><button type="button" aria-label="Cerrar radio" onClick={() => setRadioAbierta(false)}>×</button></div>
        <p className="nr-state">{reproduciendo ? '● Reproduciendo' : 'Radio en pausa'}</p>
        <label htmlFor="nr-select-radio">Estación</label>
        <select id="nr-select-radio" value={estacion} onChange={e => cambiarEstacion(Number(e.target.value))}>
          {ESTACIONES.map((e, i) => <option key={e.nombre} value={i}>{e.nombre}</option>)}
        </select>
        <audio ref={audio} preload="none" src={ESTACIONES[0].url} onError={() => { setReproduciendo(false); setRadioError('No se pudo conectar con esta emisora.'); }} onEnded={() => setReproduciendo(false)} />
        <button className="nr-radio-play" type="button" onClick={() => reproduciendo ? parar() : void reproducir()}>{reproduciendo ? '⏸ Pausar' : '▶ Reproducir'}</button>
        {radioError && <p role="alert" className="nr-error">{radioError}</p>}
      </section>}
      <button type="button" className="nr-fab nr-fab-radio" aria-label={radioAbierta ? 'Ocultar radio' : 'Abrir radio'} title="Ñackson Radio" onClick={() => setRadioAbierta(!radioAbierta)}><i className="bi bi-music-note-beamed" aria-hidden="true" /></button>
    </div>
    {regaloAbierto && <div className="nr-modal-backdrop" role="presentation" onMouseDown={e => { if (e.target === e.currentTarget) setRegaloAbierto(false); }}>
      <section className="nr-gift-modal" role="dialog" aria-modal="true" aria-label="Sorteo de ofertas vigentes">
        <button className="nr-modal-close" type="button" onClick={() => setRegaloAbierto(false)} aria-label="Cerrar">×</button>
        <h2>🎁 Descubre tu oferta</h2>
        <p>Prueba suerte y descubre una promoción <strong>que ya está vigente en Ñackson Store</strong>. El sorteo no crea cupones personales ni descuentos adicionales.</p>
        {cargando ? <p>Consultando promociones reales…</p> : errorOferta ? <p role="alert" className="nr-error">{errorOferta}</p> : ofertas.length === 0 ? <p>No hay ofertas vigentes con productos disponibles para sortear.</p> : <>
          <button className="nr-spin" type="button" onClick={sortear}>🎲 SORTEAR OFERTA</button>
          {premio && <div className="nr-prize" role="status">
            <strong>{premio.nombre}</strong>
            <p>{premio.tipoDescuento === 'PORCENTAJE' ? `${premio.valor}% de descuento` : `$${premio.valor.toLocaleString('es-CL')} de descuento`}</p>
            {premio.montoMinimoCompra != null && <p>Compra mínima: ${premio.montoMinimoCompra.toLocaleString('es-CL')}</p>}
            <p>Vigente hasta: {premio.fechaFin}</p>
            <p>Descuento aplicado por el servidor solo si se cumplen las condiciones de la promoción.</p>
            <Link to="/ofertas" onClick={() => setRegaloAbierto(false)}>Ver ofertas y productos participantes</Link>
          </div>}
        </>}
        <Link className="nr-see-offers" to="/ofertas" onClick={() => setRegaloAbierto(false)}>Consultar todas las ofertas</Link>
      </section>
    </div>}
  </>;
}
