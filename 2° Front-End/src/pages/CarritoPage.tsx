/** Carrito del Sprint 7: lógica real de React + TypeScript sobre datos visuales de ejemplo.
 * No existe compra, reserva, disponibilidad ni sincronización con Orders/Catalog. */
import { Link } from 'react-router-dom';
import { PageFrame } from '../components/PageFrame';
import { Footer } from '../components/Footer';
import { Navbar } from '../components/Navbar';
import { useCart } from '../state/CartContext';
import { formatCLP } from '../state/cartLogic';
import css from '../styles/carrito.css?inline';

export default function CarritoPage() {
  const { items, subtotal, unidades, updateQuantity, remove, clear } = useCart();
  const hasItems = items.length > 0;
  return (
    <PageFrame title="Carrito de Compras - Ñackson Store" css={css} bodyClass="d-flex flex-column min-vh-100">
      <Navbar variant="cart" />
      <div className="container mt-5 mb-5 flex-grow-1">
        <h2 className="mb-4 text-light"><i className="bi bi-cart3 me-2 text-primary"></i> Tu Carrito de Compras</h2>
        <div className="row">
          <div className="col-md-8 mb-4">
            <div className="card card-carrito p-3 shadow-sm">
              <div className="table-responsive">
                <table className="table table-dark table-hover align-middle mb-0 border-secondary bg-transparent">
                  <thead><tr className="text-muted small text-uppercase">
                    <th>Producto</th><th>Precio</th><th>Cantidad</th><th>Subtotal</th><th className="text-end">Acción</th>
                  </tr></thead>
                  <tbody id="tabla-carrito">
                    {items.map(item => (
                      <tr key={item.producto.id}>
                        <td>
                          <div className="d-flex align-items-center gap-2">
                            <i className={`${item.producto.iconClassName} fs-4`} aria-hidden="true"></i>
                            <div><span className="fw-bold">{item.producto.nombre}</span>
                              <div className="small text-white-50">Artículo de demostración</div>
                            </div>
                          </div>
                        </td>
                        <td>{formatCLP(item.producto.precioUnitario)}</td>
                        <td>
                          <div className="d-inline-flex align-items-center gap-2">
                            <button type="button" className="btn btn-outline-light btn-sm" aria-label={`Disminuir cantidad de ${item.producto.nombre}`}
                              disabled={item.cantidad === 1} onClick={() => updateQuantity(item.producto.id, item.cantidad - 1)}>−</button>
                            <input type="number" className="form-control form-control-sm bg-dark text-light border-secondary text-center"
                              style={{ width: '75px' }} min={1} step={1} value={item.cantidad}
                              aria-label={`Cantidad de ${item.producto.nombre}`}
                              onChange={e => {
                                if (!/^\d+$/.test(e.target.value)) return;
                                const value = Number(e.target.value);
                                if (Number.isSafeInteger(value) && value > 0 &&
                                    Number.isSafeInteger(subtotal - item.producto.precioUnitario * item.cantidad + item.producto.precioUnitario * value)) {
                                  updateQuantity(item.producto.id, value);
                                }
                              }} />
                            <button type="button" className="btn btn-outline-light btn-sm" aria-label={`Aumentar cantidad de ${item.producto.nombre}`}
                              disabled={!Number.isSafeInteger(item.cantidad + 1) || !Number.isSafeInteger(subtotal + item.producto.precioUnitario)}
                              onClick={() => updateQuantity(item.producto.id, item.cantidad + 1)}>+</button>
                          </div>
                        </td>
                        <td className="fw-bold text-success">{formatCLP(item.producto.precioUnitario * item.cantidad)}</td>
                        <td className="text-end"><button type="button" className="btn btn-outline-danger btn-sm" title="Eliminar artículo"
                          aria-label={`Eliminar ${item.producto.nombre} del carrito`} onClick={() => remove(item.producto.id)}>
                          <i className="bi bi-trash" aria-hidden="true"></i>
                        </button></td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
              {hasItems && <div className="text-end mt-3">
                <button type="button" className="btn btn-outline-danger btn-sm rounded-pill"
                  onClick={clear}><i className="bi bi-trash3 me-1"></i>Vaciar carrito</button>
              </div>}
            </div>
            {!hasItems && <div id="mensaje-vacio" className="alert alert-dark border border-secondary text-center py-4 mt-3" role="status">
              <i className="bi bi-cart-x fs-1 text-muted d-block mb-2"></i>
              <h5 className="text-dark">Tu carrito está vacío</h5>
              <p className="text-dark small mb-3">Explora nuestras categorías y añade tus productos físicos favoritos.</p>
              <Link to="/" className="btn btn-primary rounded-pill px-4 fw-bold">Volver al Inicio</Link>
            </div>}
          </div>
          <div className="col-md-4">
            <div className="card card-carrito shadow-sm p-3">
              <div className="card-body">
                <h5 className="card-title mb-3 text-light border-bottom border-secondary pb-2">Resumen de Compra</h5>
                <p className="d-flex justify-content-between text-white-50 mb-2">
                  <span>Unidades:</span><span className="text-light fw-bold">{unidades}</span>
                </p>
                <p className="d-flex justify-content-between text-white-50 mb-2">
                  <span>Subtotal:</span><span id="resumen-subtotal" className="text-light fw-bold">{formatCLP(subtotal)}</span>
                </p>
                <p className="d-flex justify-content-between text-white-50 mb-3">
                  <span>Costo de Envío:</span><span id="resumen-envio" className="text-light fw-bold">Por calcular</span>
                </p>
                <hr className="border-secondary" />
                <h4 className="d-flex justify-content-between text-success mb-2">
                  <span>Total parcial:</span><span id="resumen-total">{formatCLP(subtotal)}</span>
                </h4>
                <p className="text-white-50 small mb-4">Precios de ejemplo del frontend. Stock, promociones y envío se validarán con los microservicios.</p>
                {hasItems
                  ? <Link to="/checkout.html" id="btn-pagar" className="btn btn-success w-100 py-2 fw-bold rounded-pill shadow-sm">Revisar Checkout</Link>
                  : <button type="button" id="btn-pagar" disabled className="btn btn-success w-100 py-2 fw-bold rounded-pill shadow-sm">Revisar Checkout</button>}
                <Link to="/" className="btn btn-outline-light w-100 mt-2 py-2 rounded-pill">Seguir Comprando</Link>
              </div>
            </div>
          </div>
        </div>
      </div>
      <Footer variant="cart" />
    </PageFrame>
  );
}
