
/** Vista migrada desde musica.html.
 * No se ejecuta JavaScript heredado con sesiones/clientes ficticios.
 */
import { PageFrame } from '../components/PageFrame';
import { RealCatalog } from '../components/RealCatalog';
import { Footer } from '../components/Footer';
import { Navbar } from '../components/Navbar';
import css from '../styles/musica.css?inline';

export default function MusicaPage() {
  return (
    <PageFrame
      title="Música - Ñackson Store"
      css={css}
      bodyClass="d-flex flex-column min-vh-100"
    >
      <Navbar variant="category" activeCategory="musica" />

      {/* ENCABEZADO CON VIDEO DE FONDO */}
      <header
        className="position-relative overflow-hidden border-bottom border-secondary mb-4"
        style={{ minHeight: '280px', backgroundColor: '#11151d' }}
      >
        {/* Video difuminado, sin sonido y en bucle */}
        <video
          autoPlay
          muted
          loop
          playsInline
          preload="metadata"
          poster="/hero-musica.jpg"
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
          <source src="/hero-musica.mp4" type="video/mp4" />
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
          <h1 className="hero-musica-titulo text-danger mb-3">
            MÚSICA EN FORMATO FÍSICO
          </h1>

          {/* DESCRIPCIÓN NORMAL, SIN ARCOÍRIS */}
          <p className="text-white lead fs-6 mb-0">
            CDs y Vinilos coleccionables de tus artistas y bandas sonoras.
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
              categoria="MUSICA"
              titulo="Productos de Música"
            />
          </section>
        </div>
      </main>

      <Footer variant="category" />
    </PageFrame>
  );
}
