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
<main className="container my-4 flex-grow-1">{"\n"}<div className="row g-4">{"\n"}{"\n"}<div className="col-lg-8">{"\n"}<div className="d-flex justify-content-between align-items-center mb-3">{"\n"}<h3 className="text-light fs-5 mb-0">{"Resultados"}</h3>{"\n"}{"\n"}</div>{"\n"}<div className="row g-4">{"\n"}<RealCatalog categoria="ACCESORIOS" titulo="Catálogo real" mensajeCategoria="Productos filtrados desde MS-Catalog según esta sección." />{"\n"}</div>{"\n"}</div>{"\n"}{"\n"}<div className="col-lg-4">{"\n"}<div className="panel-filtros p-4 text-light">{"\n"}<h4 className="h5 text-danger mb-3 pb-2 border-bottom border-secondary"><i className="bi bi-funnel-fill me-2"></i>{" Filtrar y Ordenar"}</h4>{"\n"}{"\n"}{"\n"}{"\n"}<p className="small text-white-50">Utiliza los filtros del catálogo para refinar los resultados.</p>{"\n"}</div>{"\n"}</div>{"\n"}</div>{"\n"}</main>
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
