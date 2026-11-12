/** Vista migrada desde resultados.html. HTML visual original conservado.
 * No se ejecuta JavaScript heredado con sesiones/clientes ficticios. */
import { PageFrame } from '../components/PageFrame';
import { Footer } from '../components/Footer';
import { Navbar } from '../components/Navbar';
import css from '../styles/resultados.css?inline';

export default function ResultadosPage() {
  return (
    <PageFrame title="Ñackson Store - Resultados de Búsqueda" css={css} bodyClass="bg-dark text-light d-flex flex-column min-vh-100">
      {"\n"}

{"\n"}
<Navbar variant="results" />
{"\n"}

{"\n"}
<header className="bg-black text-white text-center py-4 mb-4 border-bottom border-danger">{"\n"}<div className="container">{"\n"}<h1 className="display-6 text-danger mb-2"><i className="bi bi-search me-2"></i>{"Resultados de Búsqueda"}</h1>{"\n"}<p className="text-white-50 small mb-0" id="textoBusqueda">{"Buscando productos..."}</p>{"\n"}</div>{"\n"}</header>
{"\n"}

{"\n"}
<main className="container mb-5 flex-grow-1">{"\n"}<div className="row g-4" id="contenedorResultados">{"\n"}{"\n"}</div>{"\n"}</main>
{"\n"}

{"\n"}
<div id="radioWidget">{"\n"}<div id="radioPanel" className="radio-panel p-3">{"\n"}<div className="d-flex justify-content-between align-items-center mb-2 border-bottom border-secondary pb-2">{"\n"}<span className="fw-bold text-danger small"><i className="bi bi-broadcast me-1"></i>{" Ñackson RADIO LIVE"}</span>{"\n"}<button type="button" className="btn-close btn-close-white btn-sm" data-legacy-onclick="toggleRadioPanel()"></button>{"\n"}</div>{"\n"}<div className="text-center mb-3">{"\n"}<span id="radioStatus" className="badge bg-secondary mb-1">{"Radio en pausa"}</span>{"\n"}<p id="radioStationName" className="text-light small mb-0 fw-bold">{"Selecciona una emisora"}</p>{"\n"}</div>{"\n"}<div className="mb-3">{"\n"}<select id="selectEstacion" className="form-select form-select-sm bg-dark text-light border-secondary">{"\n"}<option value="https://icecast.omroep.nl/radio2-bb-mp3">{"Rock Clásico (Stream Live)"}</option>{"\n"}<option value="https://n08.radiojar.com/8s5u5tpw0k8uv">{"Anime & J-Rock Hits"}</option>{"\n"}<option value="https://stream.zeno.fm/f3wvbbqmdg8uv">{"Lo-Fi & Chill Beats"}</option>{"\n"}</select>{"\n"}</div>{"\n"}<audio id="audioPlayer" preload="none"></audio>{"\n"}<div className="d-flex justify-content-center align-items-center gap-2">{"\n"}<button id="btnPlayPause" className="btn btn-danger btn-sm w-100 fw-bold" data-legacy-onclick="controlارReproduccion()">{"\n"}<i className="bi bi-play-fill fs-5"></i>{" Reproducir\n                "}</button>{"\n"}</div>{"\n"}</div>{"\n"}<button className="btn btn-danger rounded-circle shadow-lg d-flex align-items-center justify-content-center" style={{"width": "60px", "height": "60px"}} data-legacy-onclick="toggleRadioPanel()" title="Abrir Radio Ñackson">{"\n"}<i className="bi bi-music-note-beamed fs-3 text-white"></i>{"\n"}</button>{"\n"}</div>
{"\n"}

{"\n"}
<Footer variant="results" />
{"\n"}

{"\n"}
{"\n"}
{"\n"}
    </PageFrame>
  );
}
