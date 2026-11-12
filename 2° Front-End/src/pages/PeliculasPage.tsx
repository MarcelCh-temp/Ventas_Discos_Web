/** Vista migrada desde peliculas.html. HTML visual original conservado.
 * No se ejecuta JavaScript heredado con sesiones/clientes ficticios. */
import { PageFrame } from '../components/PageFrame';
import { RealCatalog } from '../components/RealCatalog';
import { Footer } from '../components/Footer';
import { Navbar } from '../components/Navbar';
import css from '../styles/peliculas.css?inline';

export default function PeliculasPage() {
  return (
    <PageFrame title="Películas - Ñackson Store" css={css} bodyClass="d-flex flex-column min-vh-100">
      {"\n"}
<Navbar variant="category" activeCategory="peliculas" />
{"\n"}
<header className="bg-dark text-white text-center py-5 border-bottom border-secondary mb-4 bg-opacity-50">{"\n"}<div className="container">{"\n"}<h1 className="text-danger fw-bold display-5 mb-2"><i className="bi bi-film me-2"></i>{" Películas en Formato Físico"}</h1>{"\n"}<p className="text-white-50 lead fs-6">{"Disfruta del cine en Blu-ray y DVD con ediciones especiales."}</p>{"\n"}</div>{"\n"}</header>
{"\n"}
<main className="container my-4 flex-grow-1">{"\n"}<div className="row g-4">{"\n"}<div className="col-lg-8">{"\n"}<div className="row g-4">{"\n"}<RealCatalog categoria="SERIES_PELICULAS_ANIME" titulo="Catalogo real: Peliculas" mensajeCategoria="Catalog agrupa series, películas y anime en una categoría oficial. Los productos se muestran según lo publicado en el servidor." />{"\n"}</div>{"\n"}</div>{"\n"}<div className="col-lg-4">{"\n"}<div className="panel-filtros p-4 text-light">{"\n"}<h4 className="h5 text-danger mb-3 pb-2 border-bottom border-secondary"><i className="bi bi-funnel-fill me-2"></i>{" Filtrar y Ordenar"}</h4>{"\n"}<button className="btn btn-outline-danger w-100 rounded-pill fw-bold btn-sm py-2">{"Aplicar Filtros"}</button>{"\n"}</div>{"\n"}</div>{"\n"}</div>{"\n"}</main>
{"\n"}
<Footer variant="category" />
{"\n"}
{"\n"}
{"\n"}
    </PageFrame>
  );
}
