import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { StockStatus } from '../components/StockStatus';
import { StoreLayout } from '../components/StoreLayout';
import { StoreNotice } from '../components/StoreNotice';
import { useAuth } from '../state/AuthContext';
import { storeApi, clp, errorMessage, type Producto, type Params } from '../services/storeApi';
import '../styles/producto-detalle.css';

const hasImage = (url: string | null): url is string =>
  Boolean(url && /^(https?:\/\/|\/)/i.test(url));

/** La categoría compartida de Catalog también contiene anime, series, películas y Blu-ray. */
function categoryFilter(product: Producto): Params {
  const params: Params = { categoria: product.categoria, page: 0, size: 24 };
  if (product.categoria === 'SERIES_PELICULAS_ANIME') {
    const genre = product.genero.trim().toLocaleLowerCase('es');
    if (genre === 'anime' || genre === 'serie' || genre === 'película' || genre === 'pelicula') {
      params.genero = product.genero;
    } else if (/blu.?ray/i.test(product.formato)) {
      params.formato = product.formato;
    }
  }
  return params;
}

export default function ProductoPage() {
  const { id } = useParams();
  const { session } = useAuth();
  const [item, setItem] = useState<Producto | null>(null);
  const [related, setRelated] = useState<Producto[]>([]);
  const [loading, setLoading] = useState(true);
  const [relatedLoading, setRelatedLoading] = useState(false);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');
  const [busy, setBusy] = useState(false);
  const [cantidad, setCantidad] = useState(1);

  useEffect(() => {
    let active = true;
    const productId = Number(id);
    setItem(null);
    setRelated([]);
    setError('');
    setNotice('');
    setCantidad(1);
    setLoading(true);
    setRelatedLoading(false);

    if (!Number.isSafeInteger(productId) || productId <= 0) {
      setError('El identificador del producto no es válido.');
      setLoading(false);
      return () => { active = false; };
    }

    void storeApi.catalog.detail(productId)
      .then(async product => {
        if (!active) return;
        setItem(product);
        setLoading(false);
        setRelatedLoading(true);
        try {
           // Primero descubrimos cuántas páginas de la misma sección existen.
           const filtros = categoryFilter(product);
           const primeraPagina = await storeApi.catalog.list(filtros);
           if (!active) return;

           // Para catálogos extensos, incorporamos otra página aleatoria.
           // Así los productos de páginas posteriores también pueden recomendarse.
           const paginas = Math.max(1, primeraPagina.totalPaginas);
           let candidatos = [...primeraPagina.contenido];
           if (paginas > 1) {
             const paginaExtra = 1 + Math.floor(Math.random() * (paginas - 1));
             try {
               const extra = await storeApi.catalog.list({ ...filtros, page: paginaExtra });
               if (!active) return;
               candidatos = candidatos.concat(extra.contenido);
             } catch {
               // Conserva los resultados disponibles si la segunda consulta falla.
             }
          }

           // Excluye el artículo abierto, evita duplicados y cambia la selección
           // cada vez que se visita el detalle de un producto.
           const sinDuplicados = Array.from(
             new Map(candidatos.filter(p => p.id !== product.id).map(p => [p.id, p])).values()
           );
           for (let i = sinDuplicados.length - 1; i > 0; i--) {
             const j = Math.floor(Math.random() * (i + 1));
             [sinDuplicados[i], sinDuplicados[j]] = [sinDuplicados[j], sinDuplicados[i]];
          }
          if (active) setRelated(sinDuplicados.slice(0, 4));
        } catch {
          // Si falla la consulta de relacionados, el detalle principal sigue disponible.
          if (active) setRelated([]);
        } finally {
          if (active) setRelatedLoading(false);
        }
      })
      .catch(e => {
        if (active) {
          setError(errorMessage(e));
          setLoading(false);
        }
      });

    return () => { active = false; };
  }, [id]);

  const stock = item?.stockActual;
  const maxCantidad = stock == null ? 200 : Math.max(0, Math.min(200, stock));
  const precio = item ? item.precioOferta ?? item.precioBase : 0;

  const changeQuantity = (next: number) => {
    if (!Number.isSafeInteger(next)) return;
    setCantidad(Math.max(1, Math.min(Math.max(1, maxCantidad), next)));
  };

  const exec = async (type: 'cart' | 'favorite') => {
    if (!session) {
      setError('Inicia sesión para añadir productos al carrito o favoritos.');
      return;
    }
    if (!item || busy) return;
    setBusy(true);
    setError('');
    setNotice('');
    try {
      if (type === 'cart') {
        const [cart, actual] = await Promise.all([
          storeApi.cart.read(session.token),
          storeApi.catalog.detail(item.id)
        ]);
        setItem(actual);
        const current = cart.items.find(i => i.productId === item.id)?.cantidad ?? 0;
        if (!actual.comprable || (actual.stockActual != null && actual.stockActual < 1)) {
          setError('Este producto ya no está disponible para comprar.');
          return;
        }
        if (actual.stockActual != null && current + cantidad > actual.stockActual) {
          setError(`Stock insuficiente: hay ${actual.stockActual} disponibles y ya tienes ${current} en el carrito.`);
          return;
        }
        await storeApi.cart.add(session.token, item.id, cantidad);
        window.dispatchEvent(new Event('nackson-cart-changed'));
        setNotice(`${cantidad} unidad(es) añadida(s) al carrito.`);
      } else {
        await storeApi.catalog.favoriteAdd(session.token, item.id);
        setNotice('Producto añadido a tus favoritos.');
      }
    } catch (e) {
      setError(errorMessage(e));
    } finally {
      setBusy(false);
    }
  };

  return (
    <StoreLayout title="Detalle del producto">
      <div className="npd">
        <nav className="npd-crumbs" aria-label="Ruta del producto">
          <Link to="/">Inicio</Link><span>/</span><span>Detalle del producto</span>
        </nav>
        <StoreNotice error={error} success={notice} />
        {loading && <p role="status" className="npd-muted">Cargando producto…</p>}
        {!loading && !item && !error && <p className="npd-muted">No se encontró el producto.</p>}
        {item && (
          <>
            <article className="npd-panel npd-product">
              <div className="npd-artwork">
                {hasImage(item.imagenUrl) ? (
                  <img src={item.imagenUrl} alt={item.nombre} />
                ) : (
                  <div className="npd-placeholder"><i className="bi bi-disc" />Imagen no disponible</div>
                )}
              </div>
              <div className="npd-info">
                <div className="npd-tags">
                  <span>{item.categoria.replaceAll('_', ' ')}</span>
                  <span>{item.formato}</span>
                  {item.precioOferta != null && <span className="npd-tag-offer">Oferta</span>}
                </div>
                <h2 className="npd-name">{item.nombre}</h2>
                <p className="npd-muted">{item.genero} · {item.formato}</p>
                <div className="npd-divider" />
                {item.precioOferta != null && (
                  <p className="npd-muted npd-old">Antes: <s>{clp(item.precioBase)}</s></p>
                )}
                <p className="npd-price">{clp(precio)}</p>
                {item.promocion && <p className="npd-offer-name">{item.promocion}</p>}
                <div className="npd-stock"><StockStatus product={item} /></div>
                <div className="npd-controls">
                  <label htmlFor="npd-quantity">Cantidad</label>
                  <div className="npd-quantity">
                    <button type="button" aria-label="Disminuir cantidad" disabled={busy || !item.comprable || cantidad <= 1} onClick={() => changeQuantity(cantidad - 1)}>−</button>
                    <input id="npd-quantity" type="number" min="1" max={Math.max(1, maxCantidad)} step="1" value={cantidad} disabled={busy || !item.comprable} onChange={e => changeQuantity(Number(e.target.value))} />
                    <button type="button" aria-label="Aumentar cantidad" disabled={busy || !item.comprable || cantidad >= maxCantidad} onClick={() => changeQuantity(cantidad + 1)}>+</button>
                  </div>
                </div>
                <div className="npd-actions">
                  <button type="button" className="npd-buy" disabled={busy || !item.comprable || maxCantidad < 1} onClick={() => void exec('cart')}><i className="bi bi-cart-plus me-2" />{busy ? 'Procesando…' : 'Agregar al carrito'}</button>
                  <button type="button" className="npd-secondary" disabled={busy} onClick={() => void exec('favorite')}><i className="bi bi-heart me-2" />Favoritos</button>
                  <Link className="npd-secondary" to="/carrito.html">Ver carrito</Link>
                </div>
                <p className="npd-muted npd-footnote">Compra académica simulada. La disponibilidad definitiva se valida en el servidor.</p>
              </div>
            </article>

            <section className="npd-panel npd-description">
              <h3>Descripción del producto</h3>
              <p>{item.descripcion || 'Descripción no disponible.'}</p>
              <h3>Información adicional</h3>
              <dl className="npd-specs">
                <div><dt>Categoría</dt><dd>{item.categoria.replaceAll('_', ' ')}</dd></div>
                <div><dt>Género</dt><dd>{item.genero || 'No especificado'}</dd></div>
                <div><dt>Formato</dt><dd>{item.formato}</dd></div>
                {item.plataforma && <div><dt>Plataforma</dt><dd>{item.plataforma}</dd></div>}
                <div><dt>Stock</dt><dd>{stock == null ? 'Por confirmar' : `${stock} unidades`}</dd></div>
              </dl>
            </section>

            <section className="npd-recommendations" aria-label="Productos relacionados">
              <div className="npd-section-heading">
                <h3>También te puede interesar</h3>
                <span>Más productos de esta categoría</span>
              </div>
              {relatedLoading && <p className="npd-muted" role="status">Buscando productos relacionados…</p>}
              {!relatedLoading && related.length === 0 && <p className="npd-muted">Por ahora no hay otros productos relacionados disponibles.</p>}
              <div className="npd-related-grid">
                {related.map(product => (
                  <article className="npd-related-card" key={product.id}>
                    <Link to={`/producto/${product.id}`} aria-label={`Ver detalle de ${product.nombre}`} className="npd-related-image">
                      {hasImage(product.imagenUrl) ? <img src={product.imagenUrl} alt={product.nombre} loading="lazy" /> : <i className="bi bi-disc" />}
                    </Link>
                    <div className="npd-related-content">
                      <p className="npd-related-meta">{product.genero} · {product.formato}</p>
                      <h4><Link to={`/producto/${product.id}`}>{product.nombre}</Link></h4>
                      <strong>{clp(product.precioOferta ?? product.precioBase)}</strong>
                      <Link to={`/producto/${product.id}`} className="npd-related-link">Ver detalle</Link>
                    </div>
                  </article>
                ))}
              </div>
            </section>
          </>
        )}
      </div>
    </StoreLayout>
  );
}
