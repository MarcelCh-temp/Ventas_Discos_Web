
/** Vista migrada desde accesorios.html.
 * No se ejecuta JavaScript heredado con sesiones/clientes ficticios.
 */
import { PageFrame } from '../components/PageFrame';
import { RealCatalog } from '../components/RealCatalog';
import { Footer } from '../components/Footer';
import { Navbar } from '../components/Navbar';
import css from '../styles/accesorios.css?inline';

export default function AccesoriosPage() {
  return (
    <PageFrame
      title="Accesorios - Ñackson Store"
      css={css}
      bodyClass="d-flex flex-column min-vh-100"
    >
      <Navbar variant="accessories" />

      {/* ENCABEZADO CON VIDEO DE FONDO */}
      <header
        className="position-relative overflow-hidden border-bottom border-secondary mb-4"
        style={{
          minHeight: '280px',
          backgroundColor: '#11151d',
        }}
      >
        {/* VIDEO DIFUMINADO, SIN SONIDO Y EN BUCLE */}
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
          <source src="/hero-accesorios.mp4" type="video/mp4" />
        </video>

        {/* CAPA OSCURA PARA MEJORAR EL CONTRASTE */}
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
          {/* TÍTULO MANUSCRITO, SIN ÍCONO */}
          <h1 className="hero-accesorios-titulo text-danger mb-3">
            ACCESORIOS Y COMPONENTES
          </h1>

          {/* DESCRIPCIÓN NORMAL */}
          <p className="text-white lead fs-6 mb-0">
            Llaveros, gorros, pines y accesorios coleccionables.
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
            <div className="d-flex justify-content-between align-items-center mb-3">
              <h3 className="text-light fs-5 mb-0">
                Resultados
              </h3>
            </div>

            <RealCatalog
              sidebarFilters
              categoria="ACCESORIOS"
              titulo="Catálogo real"
              mensajeCategoria="Productos filtrados desde MS-Catalog según esta sección."
            />
          </section>

        </div>
      </main>

      <Footer variant="accessories" />
    </PageFrame>
  );
}
