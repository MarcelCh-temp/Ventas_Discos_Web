/** Vista migrada desde index.html. HTML visual original conservado.
 * No se ejecuta JavaScript heredado con sesiones/clientes ficticios. */
import { Link } from 'react-router-dom';
import { PageFrame } from '../components/PageFrame';
import { RealCatalog } from '../components/RealCatalog';
import { Footer } from '../components/Footer';
import { Navbar } from '../components/Navbar';
import { RadioYRegalo } from '../components/RadioYRegalo';
import css from '../styles/index.css?inline';

export default function IndexPage() {
  return (
    <PageFrame title="Ñackson Store - Tienda de Entretenimiento Físico y Merchandising" css={css} bodyClass="d-flex flex-column min-vh-100">
      {"\n"}

{"\n"}
<div className="cintillo-animado-container">{"\n"}<div className="cintillo-track">{"\n"}<div className="cintillo-item"><i className="bi bi-controller me-2"></i>{" ¡TODO EN FORMATO FÍSICO Y COLECCIONABLES!"}</div>{"\n"}<div className="cintillo-item"><i className="bi bi-truck me-2"></i>{" MÉTODOS DE ENTREGA SEGÚN ORDERS"}</div>{"\n"}<div className="cintillo-item"><i className="bi bi-credit-card-2-front-fill me-2"></i>{" PAGO ACADÉMICO SIMULADO EN CHECKOUT"}</div>{"\n"}<div className="cintillo-item"><i className="bi bi-gift-fill me-2"></i>{" CONOCE LAS OFERTAS REALES DE CATALOG"}</div>{"\n"}<div className="cintillo-item"><i className="bi bi-broadcast me-2"></i>{" ESCUCHA ÑACKSON RADIO LIVE EN VIVO"}</div>{"\n"}<div className="cintillo-item"><i className="bi bi-controller me-2"></i>{" ¡TODO EN FORMATO FÍSICO Y COLECCIONABLES!"}</div>{"\n"}<div className="cintillo-item"><i className="bi bi-truck me-2"></i>{" MÉTODOS DE ENTREGA SEGÚN ORDERS"}</div>{"\n"}<div className="cintillo-item"><i className="bi bi-credit-card-2-front-fill me-2"></i>{" PAGO ACADÉMICO SIMULADO EN CHECKOUT"}</div>{"\n"}<div className="cintillo-item"><i className="bi bi-gift-fill me-2"></i>{" CONOCE LAS OFERTAS REALES DE CATALOG"}</div>{"\n"}<div className="cintillo-item"><i className="bi bi-broadcast me-2"></i>{" ESCUCHA ÑACKSON RADIO LIVE EN VIVO"}</div>{"\n"}</div>{"\n"}</div>
{"\n"}

{"\n"}
<Navbar variant="home" />
{"\n"}

{"\n"}
<header className="hero-futurista text-white text-center py-4 border-bottom border-secondary">{"\n"}<video autoPlay={true} muted={true} loop={true} playsInline={true} className="hero-video-bg">{"\n"}<source src="/video/fondo.mp4" type="video/mp4" />{"\n            Tu navegador no soporta videos HTML5.\n        "}</video>{"\n"}<div className="hero-overlay"></div>{"\n"}<div className="container position-relative z-2">{"\n"}<h1 id="tituloAkiba" className="hero-titulo-especial text-white mb-2">{"ÑACKSON STORE"}</h1>{"\n"}<p className="small mb-3 font-monospace hero-subtítulo" style={{"maxWidth": "550px", "margin": "0 auto"}}>{"Revive la nostalgia. Siente el poder de lo físico. Colecciona discos, películas, series, anime, videojuegos y artículos exclusivos con la mejor calidad."}</p>{"\n"}</div>{"\n"}</header>
{"\n"}

{"\n"}
<main className="container my-5 flex-grow-1" id="catalogo">{"\n"}<div className="row mb-4 align-items-center">{"\n"}<div className="col-md-5">{"\n"}<h2 className="mb-0 text-light fs-3">{"NUEVOS PRODUCTOS"}</h2>{"\n"}</div>{"\n"}</div>{"\n"}<div className="row g-4 mb-5">{"\n"}{"\n"}<div className="col-12"><RealCatalog titulo="Productos publicados en Catalog" limit={6} /></div>{"\n"}</div>{"\n"}{"\n"}<div className="row mb-4 align-items-center pt-3 border-top border-secondary">{"\n"}<div className="col-md-6">{"\n"}<h2 className="mb-0 text-light fs-3">{"EXPLORA POR CATEGORÍA"}</h2>{"\n"}</div>{"\n"}</div>{"\n"}<div className="row g-3">{"\n"}<div className="col-6 col-md-3">{"\n"}<Link to="/musica.html" className="categoria-card p-0 overflow-hidden position-relative rounded-3 d-block border border-secondary shadow-sm">{"\n"}<img src="/img/cat_musica.jpg" alt="Música" className="w-100 object-fit-cover" style={{"height": "110px", "opacity": "0.75"}} />{"\n"}<div className="position-absolute top-50 start-50 translate-middle w-100 text-center px-2">{"\n"}<h3 className="h6 mb-0 text-white fw-bold text-shadow">{"MÚSICA"}</h3>{"\n"}</div>{"\n"}</Link>{"\n"}</div>{"\n"}<div className="col-6 col-md-3">{"\n"}<Link to="/anime.html" className="categoria-card p-0 overflow-hidden position-relative rounded-3 d-block border border-secondary shadow-sm">{"\n"}<img src="/img/cat_anime.jpg" alt="Anime" className="w-100 object-fit-cover" style={{"height": "110px", "opacity": "0.75"}} />{"\n"}<div className="position-absolute top-50 start-50 translate-middle w-100 text-center px-2">{"\n"}<h3 className="h6 mb-0 text-white fw-bold text-shadow">{"ANIME"}</h3>{"\n"}</div>{"\n"}</Link>{"\n"}</div>{"\n"}<div className="col-6 col-md-3">{"\n"}<Link to="/videojuegos.html" className="categoria-card p-0 overflow-hidden position-relative rounded-3 d-block border border-secondary shadow-sm">{"\n"}<img src="/img/cat_videojuegos.jpg" alt="Videojuegos" className="w-100 object-fit-cover" style={{"height": "110px", "opacity": "0.75"}} />{"\n"}<div className="position-absolute top-50 start-50 translate-middle w-100 text-center px-2">{"\n"}<h3 className="h6 mb-0 text-white fw-bold text-shadow">{"VIDEOJUEGOS"}</h3>{"\n"}</div>{"\n"}</Link>{"\n"}</div>{"\n"}<div className="col-6 col-md-3">{"\n"}<Link to="/peliculas.html" className="categoria-card p-0 overflow-hidden position-relative rounded-3 d-block border border-secondary shadow-sm">{"\n"}<img src="/img/cat_peliculas.jpg" alt="Películas" className="w-100 object-fit-cover" style={{"height": "110px", "opacity": "0.75"}} />{"\n"}<div className="position-absolute top-50 start-50 translate-middle w-100 text-center px-2">{"\n"}<h3 className="h6 mb-0 text-white fw-bold text-shadow">{"PELÍCULAS"}</h3>{"\n"}</div>{"\n"}</Link>{"\n"}</div>{"\n"}<div className="col-6 col-md-3">{"\n"}<Link to="/series.html" className="categoria-card p-0 overflow-hidden position-relative rounded-3 d-block border border-secondary shadow-sm">{"\n"}<img src="/img/cat_series.jpg" alt="Series" className="w-100 object-fit-cover" style={{"height": "110px", "opacity": "0.75"}} />{"\n"}<div className="position-absolute top-50 start-50 translate-middle w-100 text-center px-2">{"\n"}<h3 className="h6 mb-0 text-white fw-bold text-shadow">{"SERIES"}</h3>{"\n"}</div>{"\n"}</Link>{"\n"}</div>{"\n"}<div className="col-6 col-md-3">{"\n"}<Link to="/bluray.html" className="categoria-card p-0 overflow-hidden position-relative rounded-3 d-block border border-secondary shadow-sm">{"\n"}<img src="/img/cat_bluray.jpg" alt="Blu-ray / DVD" className="w-100 object-fit-cover" style={{"height": "110px", "opacity": "0.75"}} />{"\n"}<div className="position-absolute top-50 start-50 translate-middle w-100 text-center px-2">{"\n"}<h3 className="h6 mb-0 text-white fw-bold text-shadow">{"BLU-RAY / DVD"}</h3>{"\n"}</div>{"\n"}</Link>{"\n"}</div>{"\n"}<div className="col-6 col-md-3">{"\n"}<Link to="/accesorios.html" className="categoria-card p-0 overflow-hidden position-relative rounded-3 d-block border border-secondary shadow-sm">{"\n"}<img src="/img/cat_accesorios.jpg" alt="Accesorios" className="w-100 object-fit-cover" style={{"height": "110px", "opacity": "0.75"}} />{"\n"}<div className="position-absolute top-50 start-50 translate-middle w-100 text-center px-2">{"\n"}<h3 className="h6 mb-0 text-white fw-bold text-shadow">{"ACCESORIOS"}</h3>{"\n"}</div>{"\n"}</Link>{"\n"}</div>{"\n"}<div className="col-6 col-md-3">{"\n"}<Link to="/merchandising.html" className="categoria-card p-0 overflow-hidden position-relative rounded-3 d-block border border-secondary shadow-sm">{"\n"}<img src="/img/cat_merch.jpg" alt="Merchandising" className="w-100 object-fit-cover" style={{"height": "110px", "opacity": "0.75"}} />{"\n"}<div className="position-absolute top-50 start-50 translate-middle w-100 text-center px-2">{"\n"}<h3 className="h6 mb-0 text-white fw-bold text-shadow">{"MERCHANDISING"}</h3>{"\n"}</div>{"\n"}</Link>{"\n"}</div>{"\n"}</div>{"\n"}</main>
{"\n"}

{"\n"}
<RadioYRegalo />

<Footer variant="home" />
{"\n"}

{"\n"}
{"\n"}
{"\n"}
{"\n"}
    </PageFrame>
  );
}
