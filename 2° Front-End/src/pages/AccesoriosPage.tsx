/** Vista migrada desde accesorios.html. HTML visual original conservado.
 * No se ejecuta JavaScript heredado con sesiones/clientes ficticios. */
import { PageFrame } from '../components/PageFrame';
import { RealCatalog } from '../components/RealCatalog';
import { Footer } from '../components/Footer';
import { Navbar } from '../components/Navbar';
import css from '../styles/accesorios.css?inline';

export default function AccesoriosPage() {
  return (
    <PageFrame title="Accesorios - Ñackson Store" css={css} bodyClass="d-flex flex-column min-vh-100">
      {"\n"}

{"\n"}
<Navbar variant="accessories" />
{"\n"}

{"\n"}
<header className="bg-dark text-white text-center py-5 border-bottom border-secondary mb-4 bg-opacity-50">{"\n"}<div className="container">{"\n"}<h1 className="text-danger fw-bold display-5 mb-2"><i className="bi bi-watch me-2"></i>{" Accesorios y Componentes"}</h1>{"\n"}<p className="text-white-50 lead fs-6">{"Llaveros, gorros, pines y accesorios coleccionables."}</p>{"\n"}</div>{"\n"}</header>
{"\n"}

{"\n"}
<main className="container my-4 flex-grow-1">{"\n"}<div className="row g-4">{"\n"}{"\n"}<div className="col-lg-8">{"\n"}<div className="d-flex justify-content-between align-items-center mb-3">{"\n"}<h3 className="text-light fs-5 mb-0">{"Resultados"}</h3>{"\n"}<span className="badge bg-danger px-3 py-2 rounded-pill font-monospace">{"1 producto"}</span>{"\n"}</div>{"\n"}<div className="row g-4">{"\n"}<RealCatalog titulo="Catálogo real" mensajeCategoria="La API definitiva aún no define accesorios como categoría independiente. Se muestra el catálogo oficial completo, sin inventar artículos." />{"\n"}</div>{"\n"}</div>{"\n"}{"\n"}<div className="col-lg-4">{"\n"}<div className="panel-filtros p-4 text-light">{"\n"}<h4 className="h5 text-danger mb-3 pb-2 border-bottom border-secondary"><i className="bi bi-funnel-fill me-2"></i>{" Filtrar y Ordenar"}</h4>{"\n"}<div className="mb-3">{"\n"}<label className="form-label small fw-bold text-light">{"Ordenar Alfabéticamente"}</label>{"\n"}<select className="form-select bg-dark text-light border-secondary form-select-sm">{"\n"}<option>{"De la A a la Z"}</option>{"\n"}<option>{"De la Z a la A"}</option>{"\n"}</select>{"\n"}</div>{"\n"}<div className="mb-3">{"\n"}<label className="form-label small fw-bold text-light">{"Criterio Global"}</label>{"\n"}<select className="form-select bg-dark text-light border-secondary form-select-sm">{"\n"}<option>{"Lo más global / Tendencia"}</option>{"\n"}<option>{"Precio: Menor a Mayor"}</option>{"\n"}<option>{"Precio: Mayor a Menor"}</option>{"\n"}</select>{"\n"}</div>{"\n"}<div className="mb-4">{"\n"}<label className="form-label small fw-bold text-light">{"Rango de Precio"}</label>{"\n"}<select className="form-select bg-dark text-light border-secondary form-select-sm">{"\n"}<option>{"Todos los precios"}</option>{"\n"}<option>{"Menos de $20.000"}</option>{"\n"}<option>{"$20.000 a $50.000"}</option>{"\n"}<option>{"Más de $50.000"}</option>{"\n"}</select>{"\n"}</div>{"\n"}<button className="btn btn-outline-danger w-100 rounded-pill fw-bold btn-sm py-2">{"Aplicar Filtros"}</button>{"\n"}</div>{"\n"}</div>{"\n"}</div>{"\n"}</main>
{"\n"}

{"\n"}
<Footer variant="accessories" />
{"\n"}

{"\n"}
{"\n"}
{"\n"}
    </PageFrame>
  );
}
