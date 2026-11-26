
/** Vista migrada desde peliculas.html.
 * No se ejecuta JavaScript heredado con sesiones/clientes ficticios.
 */
import { PageFrame } from '../components/PageFrame';
import { RealCatalog } from '../components/RealCatalog';
import { Footer } from '../components/Footer';
import { Navbar } from '../components/Navbar';
import css from '../styles/peliculas.css?inline';

export default function PeliculasPage() {
  return (
    <PageFrame
      title="Películas - Ñackson Store"
      css={css}
      bodyClass="d-flex flex-column min-vh-100"
    >
      <Navbar variant="category" activeCategory="peliculas" />

      {/* ENCABEZADO CON VIDEO DE FONDO */}
      <header
        className="position-relative overflow-hidden border-bottom border-secondary mb-4"
        style={{
          minHeight: '280px',
          backgroundColor: '#11151d',
        }}
      >
        {/* Video difuminado, sin sonido y en bucle */}
        <video
          autoPlay
          muted
          loop
          playsInline
          preload="metadata"
          aria-hidden="true"
          className="position-absolute top-0 start-0 w-100 h-100"
          style={{
            objectFit: 'cover',
            objectPosition: 'center',
            filter: 'blur(6px) brightness(0.65)',
            transform: 'scale(1.08)',
            pointerEvents: 'none',
          }}
        >
          <source src="/hero-peliculas.mp4" type="video/mp4" />
        </video>

        {/* Capa oscura para mejorar el contraste */}
        <div
          className="position-absolute top-0 start-0 w-100 h-100"
          style={{
            backgroundColor: 'rgba(0, 0, 0, 0.35)',
            pointerEvents: 'none',
          }}
        />

        {/* TEXTO ENCIMA DEL VIDEO */}
        <div
          className="container position-relative text-center text-white d-flex flex-column justify-content-center"
          style={{
            minHeight: '280px',
            zIndex: 1,
          }}
        >
          {/* TÍTULO CON FUENTE MANUSCRITA */}
          <h1 className="hero-peliculas-titulo text-danger mb-3">
            PELÍCULAS EN FORMATO FÍSICO
          </h1>

          {/* DESCRIPCIÓN NORMAL */}
          <p className="text-white lead fs-6 mb-0">
            Disfruta del cine en Blu-ray y DVD con ediciones especiales.
          </p>
        </div>
      </header>

      {/* CATÁLOGO Y FILTROS */}
      <main className="container-fluid px-3 px-lg-4 my-4 flex-grow-1">
        <div className="row g-3">

          {/* FILTROS - IZQUIERDA */}
          <aside className="col-12 col-lg-3">
            <div id="nackson-category-filter-panel" />
          </aside>

          {/* PRODUCTOS - DERECHA */}
          <section className="col-12 col-lg-9">
            <RealCatalog
              sidebarFilters
              categoria="SERIES_PELICULAS_ANIME"
              genero="Película"
              titulo="Catalogo real: Peliculas"
              mensajeCategoria="Productos filtrados desde MS-Catalog según esta sección."
            />
          </section>

        </div>
      </main>

      <Footer variant="category" />
    </PageFrame>
  );
}
