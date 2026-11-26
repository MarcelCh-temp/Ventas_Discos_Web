

import { useCallback, useEffect, useRef, useState } from 'react';

import { Link } from 'react-router-dom';

import { createPortal } from 'react-dom';

import { useAuth } from '../state/AuthContext';

import { storeApi, clp, errorMessage, type Categoria, type Producto } from '../services/storeApi';

import type { Params } from '../services/storeApi';

import { StockStatus } from './StockStatus';

type Props = {

  categoria?: Categoria;

  genero?: string;

  formato?: string;

  q?: string;

  titulo?: string;

  limit?: number;

  mensajeCategoria?: string;

  sidebarFilters?: boolean;

};

type CategoryFilters = {
  precioMin: string;
  precioMax: string;
  disponibilidad: string;
  busqueda: string;
  genero: string;
  formato: string;
};

const EMPTY_CATEGORY_FILTERS: CategoryFilters = {
  precioMin: '', precioMax: '', disponibilidad: '', busqueda: '', genero: '', formato: ''
};

type SavedCategoryState = {
  page: number;
  filters: CategoryFilters;
  applied: CategoryFilters;
  sortOrder: string;
};

// Las categorías se recuerdan por URL. El catálogo de Inicio queda fuera.
function categoryStorageKey(): string {
  return `nackson:categoria:v1:${window.location.pathname}`;
}

function readCategoryState(): SavedCategoryState | null {
  try {
    const json = sessionStorage.getItem(categoryStorageKey());
    if (!json) return null;
    const value: unknown = JSON.parse(json);
    if (!value || typeof value !== 'object') return null;
    const saved = value as Partial<SavedCategoryState>;
    const cleanFilters = (candidate: unknown): CategoryFilters => {
      const source = candidate && typeof candidate === 'object' ? candidate as Record<string, unknown> : {};
      return {
        precioMin: typeof source.precioMin === 'string' ? source.precioMin : '',
        precioMax: typeof source.precioMax === 'string' ? source.precioMax : '',
        disponibilidad: typeof source.disponibilidad === 'string' ? source.disponibilidad : '',
        busqueda: typeof source.busqueda === 'string' ? source.busqueda : '',
        genero: typeof source.genero === 'string' ? source.genero : '',
        formato: typeof source.formato === 'string' ? source.formato : ''
      };
    };
    return {
      page: Number.isInteger(saved.page) && (saved.page as number) >= 0 ? saved.page as number : 0,
      filters: cleanFilters(saved.filters),
      applied: cleanFilters(saved.applied),
      sortOrder: typeof saved.sortOrder === 'string' ? saved.sortOrder : ''
    };
  } catch {
    return null;
  }
}

/** Catálogo de la API real. Ningún artículo ficticio; la fuente de verdad es Catalog. */

export function RealCatalog({

  categoria,

  genero,

  formato,

  q,

  titulo,

  limit,

  mensajeCategoria,

  sidebarFilters = false

}: Props) {

  const auth = useAuth();

  const [items, setItems] = useState<Producto[]>([]);

  const [savedCategory] = useState<SavedCategoryState | null>(() => sidebarFilters ? readCategoryState() : null);

  const [page, setPage] = useState(savedCategory?.page ?? 0);

  const [total, setTotal] = useState(0);

  const [loading, setLoading] = useState(true);

  const [failure, setFailure] = useState('');

  const [notice, setNotice] = useState('');

  const [busy, setBusy] = useState<number | null>(null);

  const [sidebarTarget, setSidebarTarget] = useState<HTMLElement | null>(null);

  const [sortOrder, setSortOrder] = useState(savedCategory?.sortOrder ?? '');

  useEffect(() => {

    if (sidebarFilters) setSidebarTarget(document.getElementById('nackson-category-filter-panel'));

  }, [sidebarFilters]);

  const busyRef = useRef(false);

  const [filters, setFilters] = useState<CategoryFilters>(savedCategory?.filters ?? { ...EMPTY_CATEGORY_FILTERS });

  const [applied, setApplied] = useState<CategoryFilters>(savedCategory?.applied ?? { ...EMPTY_CATEGORY_FILTERS });

  // Paginación, búsqueda, filtros y orden sobreviven a la navegación al detalle.
  useEffect(() => {
    if (!sidebarFilters) return;
    try {
      sessionStorage.setItem(categoryStorageKey(), JSON.stringify({ page, filters, applied, sortOrder }));
    } catch {
      // El catálogo funciona aunque el almacenamiento esté bloqueado.
    }
  }, [sidebarFilters, page, filters, applied, sortOrder]);

  const rememberCategoryScroll = () => {
    if (!sidebarFilters) return;
    try {
      sessionStorage.setItem(`${categoryStorageKey()}:returnY`, String(window.scrollY));
    } catch {
      // Sin almacenamiento se conserva el desplazamiento normal del navegador.
    }
  };

  const scrollRestored = useRef(false);
  useEffect(() => {
    if (!sidebarFilters || loading || failure || scrollRestored.current) return;
    scrollRestored.current = true;
    const key = `${categoryStorageKey()}:returnY`;
    const savedY = sessionStorage.getItem(key);
    if (savedY === null) return;
    sessionStorage.removeItem(key);
    const y = Number(savedY);
    if (!Number.isFinite(y) || y < 0) return;
    requestAnimationFrame(() => requestAnimationFrame(() => window.scrollTo({ top: y, behavior: 'auto' })));
  }, [sidebarFilters, loading, failure]);

  const requestId = useRef(0);

  const reload = useCallback(async () => {

    const currentRequest = ++requestId.current;

    const params: Params = {

      page,

      size: limit || 12,

      categoria,

      genero: genero || applied.genero,

      formato: formato || applied.formato,

      q: applied.busqueda.trim() || q,

      precioMin: applied.precioMin,

      precioMax: applied.precioMax,

      disponibilidad: applied.disponibilidad

    };

    setLoading(true);

    setFailure('');

    setItems([]);

    setTotal(0);

    try {

      const data = await storeApi.catalog.list(params);

      if (currentRequest !== requestId.current) return;

      setItems(data.contenido);

      setTotal(data.totalPaginas);

    } catch (e) {

      if (currentRequest !== requestId.current) return;

      setFailure(errorMessage(e));

    } finally {

      if (currentRequest === requestId.current) {

        setLoading(false);

      }

    }

  }, [page, limit, categoria, genero, formato, q, applied]);

  useEffect(() => {

    void reload();

    return () => {

      requestId.current += 1;

    };

  }, [reload]);

  const submitFilters = (event: React.FormEvent<HTMLFormElement>) => {

    event.preventDefault();

    if (filters.precioMin !== '' && filters.precioMax !== '' && Number(filters.precioMin) > Number(filters.precioMax)) {

      setNotice('El precio mínimo no puede superar el máximo.');

      return;

    }

    setNotice('');

    setPage(0);

    setApplied({ ...filters });

  };

  const resetFilters = () => {

    const empty = { precioMin: '', precioMax: '', disponibilidad: '', busqueda: '', genero: '', formato: '' };

    setFilters(empty);

    setApplied(empty);

    setSortOrder('');

    setNotice('');

    setPage(0);

  };

  // El backend pagina los resultados. El ordenamiento local se aplica a la página visible.

  const visibleItems = [...items].sort((a, b) => {

    if (sortOrder === 'priceAsc') return (a.precioOferta ?? a.precioBase) - (b.precioOferta ?? b.precioBase);

    if (sortOrder === 'priceDesc') return (b.precioOferta ?? b.precioBase) - (a.precioOferta ?? a.precioBase);

    if (sortOrder === 'nameAsc') return a.nombre.localeCompare(b.nombre, 'es');

    return 0;

  });

  const filterSidebar = (

    <div className="panel-filtros nackson-category-filters p-4">

      <h3 className="h5 text-danger mb-3 pb-2 border-bottom border-secondary">

        <i className="bi bi-funnel-fill me-2" /> Filtrar y ordenar

      </h3>

      <form data-nackson-react-form onSubmit={submitFilters}>

        <label className="d-block mb-3">Buscar producto

          <input className="form-control mt-1" type="search" value={filters.busqueda}

            placeholder={categoria === 'MUSICA' ? 'Artista o álbum' : 'Nombre del producto'}

            onChange={e => setFilters({ ...filters, busqueda: e.target.value })} />

        </label>

        <div className="row g-2 mb-3">

          <label className="col-6">Precio mínimo ($)

            <input className="form-control mt-1" type="number" min="0" step="1" value={filters.precioMin}

              onChange={e => setFilters({ ...filters, precioMin: e.target.value })} />

          </label>

          <label className="col-6">Precio máximo ($)

            <input className="form-control mt-1" type="number" min="0" step="1" value={filters.precioMax}

              onChange={e => setFilters({ ...filters, precioMax: e.target.value })} />

          </label>

        </div>

        <label className="d-block mb-3">Disponibilidad

          <select className="form-select mt-1" value={filters.disponibilidad}

            onChange={e => setFilters({ ...filters, disponibilidad: e.target.value })}>

            <option value="">Todas</option>

            <option value="DISPONIBLE">Disponible</option>

            <option value="STOCK_CRITICO">Stock crítico</option>

            <option value="AGOTADO">Agotado</option>

          </select>

        </label>

        {!genero && (

          <label className="d-block mb-3">Género

            <input className="form-control mt-1" value={filters.genero}

              placeholder={categoria === 'MUSICA' ? 'Ej. Rock' : 'Género del catálogo'}

              onChange={e => setFilters({ ...filters, genero: e.target.value })} />

          </label>

        )}

        {!formato && (

          <label className="d-block mb-3">Formato

            <input className="form-control mt-1" value={filters.formato}

              placeholder="Ej. CD, DVD, Blu-ray"

              onChange={e => setFilters({ ...filters, formato: e.target.value })} />

          </label>

        )}

        <label className="d-block mb-3">Ordenar resultados de esta página

          <select className="form-select mt-1" value={sortOrder} onChange={e => setSortOrder(e.target.value)}>

            <option value="">Orden original</option>

            <option value="priceAsc">Menor precio primero</option>

            <option value="priceDesc">Mayor precio primero</option>

            <option value="nameAsc">Nombre A-Z</option>

          </select>

        </label>

        <div className="d-flex gap-2 flex-wrap">

          <button type="submit" className="btn btn-danger flex-fill"><i className="bi bi-funnel me-1" />Filtrar</button>

          <button type="button" className="btn btn-outline-light flex-fill" onClick={resetFilters}>

            <i className="bi bi-arrow-counterclockwise me-1" />Limpiar

          </button>

        </div>

      </form>

    </div>

  );

  const add = async (id: number) => {

    if (!auth.session) {

      setNotice('Inicia sesión para añadir productos al carrito de Orders.');

      return;

    }

    if (busyRef.current) return;

    busyRef.current = true;

    setBusy(id);

    setNotice('');

    try {

      // El backend sigue validando definitivamente el stock. Esta prevalidación

      // proporciona un aviso inmediato y comprensible antes de enviarlo.

      const [cart, latest] = await Promise.all([

        storeApi.cart.read(auth.session.token),

        storeApi.catalog.detail(id)

      ]);

      const current = cart.items.find(item => item.productId === id)?.cantidad ?? 0;

      if (latest.stockActual != null && current >= latest.stockActual) {

        setNotice(`No puedes agregar más: ya tienes ${current} unidades en el carrito y solo hay ${latest.stockActual} disponibles.`);

        return;

      }

      if (!latest.comprable) {

        setNotice('Este producto ya no está disponible para comprar.');

        return;

      }

      await storeApi.cart.add(auth.session.token, id, 1);

      setNotice('Producto añadido al carrito real.');

      window.dispatchEvent(new Event('nackson-cart-changed'));

    } catch (e) {

      setNotice('No se pudo agregar el producto. ' + errorMessage(e));

    } finally {

      busyRef.current = false;

      setBusy(null);

    }

  };

  return (

    <section

      aria-label={titulo || 'Catálogo de productos'}

      className="w-100"

    >

      {titulo && (

        <h2 className="h4 text-light mb-3">

          {titulo}

        </h2>

      )}

      {mensajeCategoria && (

        <p className="small text-info">

          {mensajeCategoria}

        </p>

      )}

      {sidebarFilters && sidebarTarget && createPortal(filterSidebar, sidebarTarget)}

      {/* FILTROS DEL CATÁLOGO (busqueda global) */}

      {!sidebarFilters && <form

        data-nackson-react-form

        className="row g-2 mb-3"

        onSubmit={e => {

          e.preventDefault();

          if (

            filters.precioMin !== '' &&

            filters.precioMax !== '' &&

            Number(filters.precioMin) > Number(filters.precioMax)

          ) {

            setNotice('El precio mínimo no puede superar el máximo.');

            return;

          }

          setNotice('');

          setPage(0);

          setApplied({ ...filters });

        }}

      >

        <div className="col-sm-3">

          <label className="d-block text-light small mb-1">

            Precio mínimo ($)

            <input

              aria-label="Precio mínimo"

              type="number"

              min="0"

              step="1"

              value={filters.precioMin}

              placeholder="Precio mínimo"

              className="form-control bg-dark text-light border-secondary"

              onChange={e =>

                setFilters({

                  ...filters,

                  precioMin: e.target.value

                })

              }

            />

          </label>

        </div>

        <div className="col-sm-3">

          <label className="d-block text-light small mb-1">

            Precio máximo ($)

            <input

              aria-label="Precio máximo"

              type="number"

              min="0"

              step="1"

              value={filters.precioMax}

              placeholder="Precio máximo"

              className="form-control bg-dark text-light border-secondary"

              onChange={e =>

                setFilters({

                  ...filters,

                  precioMax: e.target.value

                })

              }

            />

          </label>

        </div>

        <div className="col-sm-4">

          <label className="d-block text-light small mb-1">

            Disponibilidad

            <select

              aria-label="Disponibilidad"

              className="form-select bg-dark text-light border-secondary"

              value={filters.disponibilidad}

              onChange={e =>

                setFilters({

                  ...filters,

                  disponibilidad: e.target.value

                })

              }

            >

              <option value="">

                Cualquier disponibilidad

              </option>

              <option value="DISPONIBLE">

                Disponible

              </option>

              <option value="STOCK_CRITICO">

                Stock crítico

              </option>

              <option value="AGOTADO">

                Agotado

              </option>

            </select>

          </label>

        </div>

        <div className="col-sm-2 d-flex align-items-end">

          <button

            type="submit"

            className="btn btn-outline-danger w-100 mb-1"

          >

            Filtrar

          </button>

        </div>

      </form>}

      {/* ESTADOS DE CARGA Y MENSAJES */}

      {loading && (

        <p role="status" className="text-light">

          Cargando productos desde Catalog…

        </p>

      )}

      {failure && (

        <div role="alert" className="alert alert-warning">

          {failure}

          <button

            type="button"

            onClick={() => void reload()}

            className="btn btn-outline-dark btn-sm ms-2"

          >

            Reintentar

          </button>

        </div>

      )}

      {notice && (

        <p role="status" className="alert alert-info">

          {notice}

        </p>

      )}

      {!loading && !failure && items.length === 0 && (

        <p className="text-light">

          No hay productos que coincidan con los filtros en Catalog.

        </p>

      )}

      {/* CATÁLOGO: 4 COLUMNAS EN PANTALLAS GRANDES */}

      <div className="row g-3">

        {visibleItems.map(product => (

          <div

            key={product.id}

            className="col-12 col-sm-6 col-lg-4 col-xl-3"

          >

            <div

              className="card bg-dark border-secondary h-100 text-light shadow-sm overflow-hidden"

              style={{

                borderRadius: '6px',

                display: 'flex',

                flexDirection: 'column'

              }}

            >

              {/* PORTADA CUADRADA Y UNIFORME */}

              <Link
                to={`/producto/${product.id}`}
                onClick={rememberCategoryScroll}
                className="d-block text-decoration-none"
                aria-label={`Ver detalles de ${product.nombre}`}
                title={`Ver detalles de ${product.nombre}`}
                style={{ cursor: 'pointer' }}
              >
              <div

                className=""

                style={{

                  width: '100%',

                  aspectRatio: '1 / 1',

                  overflow: 'hidden',

                  display: 'flex',

                  alignItems: 'center',

                  justifyContent: 'center'

                }}

              >

                {product.imagenUrl &&
                /^(https?:\/\/|\/)/i.test(product.imagenUrl) ? (

                  <img

                    src={product.imagenUrl}

                    alt={product.nombre}

                    loading="lazy"

                    style={{

                      width: '100%',

                      height: '100%',

                      display: 'block',

                      objectFit: 'contain',

                      objectPosition: 'center'

                    }}

                  />

                ) : (

                  <i className="bi bi-disc fs-1 text-danger" />

                )}

              </div>
              </Link>

              {/* INFORMACIÓN DEL PRODUCTO */}

              <div

                className="card-body d-flex flex-column"

                style={{

                  padding: '12px',

                  gap: '4px'

                }}

              >

                {/* NOMBRE */}

                <h3

                  className="h6 fw-bold mb-1"

                  style={{

                    lineHeight: '1.35',

                    minHeight: '42px',

                    overflowWrap: 'anywhere'

                  }}

                >

                  <Link
                    to={`/producto/${product.id}`}
                    onClick={rememberCategoryScroll}
                    className="text-decoration-none"
                    style={{ color: 'inherit', cursor: 'pointer' }}
                    title={`Ver detalles de ${product.nombre}`}
                  >
                    {product.nombre}
                  </Link>
                </h3>

                {/* GÉNERO Y FORMATO */}

                <p className="text-white-50 small mb-2">

                  {product.genero} · {product.formato}

                </p>

                {/* DESCRIPCIÓN */}

                <p

                  className="small text-white-50 flex-grow-1 mb-2"

                  style={{

                    lineHeight: '1.45'

                  }}

                >

                  {product.descripcion}

                </p>

                {/* PRECIO */}

                <div className="fw-bold fs-5 text-success">

                  {clp(product.precioOferta ?? product.precioBase)}

                </div>

                {/* PRECIO ORIGINAL EN PROMOCIONES */}

                {product.precioOferta != null && (

                  <div className="text-white-50 small">

                    <s>{clp(product.precioBase)}</s>

                    {' · '}

                    {product.promocion}

                  </div>

                )}

                {/* DISPONIBILIDAD */}

                <StockStatus product={product} />

                {/* AGREGAR AL CARRITO: ACCIÓN INDEPENDIENTE */}

                <div className="d-flex gap-2 mt-auto">

                  <button

                    type="button"

                    className="btn btn-danger btn-sm w-100"

                    disabled={

                      !product.comprable ||

                      busy === product.id

                    }

                    onClick={() => void add(product.id)}

                  >

                    {busy === product.id

                      ? 'Añadiendo…'

                      : 'Agregar'}

                  </button>

                </div>

              </div>

            </div>

          </div>

        ))}

      </div>

      {/* PAGINACIÓN */}

      {!loading && total > 1 && (

        <div className="d-flex justify-content-center align-items-center gap-3 mt-4">

          <button

            type="button"

            className="btn btn-outline-light"

            disabled={page === 0}

            onClick={() => setPage(p => p - 1)}

          >

            Anterior

          </button>

          <span className="text-light">

            Página {page + 1} de {total}

          </span>

          <button

            type="button"

            className="btn btn-outline-light"

            disabled={page + 1 >= total}

            onClick={() => setPage(p => p + 1)}

          >

            Siguiente

          </button>

        </div>

      )}

    </section>

  );

}
