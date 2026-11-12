/** Sólo vista previa de un carrito de demostración; jamás procesa pagos ni pedidos. */
import { Link } from 'react-router-dom';
import { PageFrame } from '../components/PageFrame';
import { Footer } from '../components/Footer';
import { Navbar } from '../components/Navbar';
import { useCart } from '../state/CartContext';
import { formatCLP } from '../state/cartLogic';
import css from '../styles/checkout.css?inline';

export default function CheckoutPage() {
  const { items, subtotal } = useCart();
  return (
    <PageFrame title="Ñackson Store - Checkout & Soporte" css={css} bodyClass="d-flex flex-column min-vh-100">
      <Navbar variant="checkout" />
      <main className="container my-5 flex-grow-1">
        <div className="text-center mb-5">
          <h1 className="text-light display-5">FINALIZAR COMPRA &amp; SOPORTE</h1>
          <p className="text-light opacity-75">Vista previa del carrito en React + TypeScript. No hay compras ni pedidos confirmados.</p>
        </div>
        <div className="row g-4">
          <div className="col-lg-6">
            <div className="card card-custom h-100 p-4">
              <div className="d-flex align-items-center gap-2 mb-3 border-bottom border-secondary pb-3">
                <i className="bi bi-bag-check-fill text-success fs-3"></i>
                <div><h3 className="h4 mb-0 text-success">1. Módulo de Pedidos</h3>
                  <span className="badge bg-secondary">Pendiente de integración con Orders y Catalog</span></div>
              </div>
              <div className="mb-4">
                <h5 className="text-light">Resumen del Carrito</h5>
                {items.length === 0
                  ? <div className="p-3 bg-dark rounded border border-secondary mt-2 text-light">
                      No hay productos en el carrito. <Link to="/carrito.html">Volver al carrito</Link>
                    </div>
                  : items.map(item => <div key={item.producto.id}
                        className="d-flex justify-content-between align-items-center p-3 bg-dark rounded border border-secondary mt-2 gap-3">
                      <div className="text-light"><strong>{item.producto.nombre}</strong>
                        <div className="text-muted small">{item.cantidad} × {formatCLP(item.producto.precioUnitario)}</div>
                      </div>
                      <span className="fw-bold text-success fs-5 text-nowrap">{formatCLP(item.producto.precioUnitario * item.cantidad)}</span>
                    </div>)}
              </div>
              <div className="mt-auto">
                <div className="d-flex justify-content-between align-items-center text-light mb-1">
                  <span>Subtotal provisional:</span><strong className="text-success">{formatCLP(subtotal)}</strong>
                </div>
                <p className="small text-light opacity-75 mb-3">Envío, disponibilidad y precio definitivo aún no se calculan.</p>
                <button type="button" disabled className="btn btn-success btn-lg w-100 fw-bold shadow-sm py-3">
                  <i className="bi bi-credit-card-fill me-2"></i> Pago pendiente de integración
                </button>
                <Link to="/carrito.html" className="btn btn-outline-light w-100 mt-2 rounded-pill">Editar carrito</Link>
              </div>
            </div>
          </div>
          <div className="col-lg-6">
            <div className="card card-custom h-100 p-4">
              <div className="d-flex align-items-center gap-2 mb-3 border-bottom border-secondary pb-3">
                <i className="bi bi-headset text-primary fs-3"></i>
                <div><h3 className="h4 mb-0 text-primary">2. Mesa de Ayuda</h3>
                  <span className="badge bg-secondary">Pendiente de integración</span></div>
              </div>
              <form>
                <div className="mb-3"><label htmlFor="asuntoTicket" className="form-label text-light small fw-bold">Asunto de la consulta</label>
                  <input type="text" id="asuntoTicket" className="form-control bg-dark text-light border-secondary"
                    placeholder="Ej: Consulta de envío o stock" required /></div>
                <div className="mb-3"><label htmlFor="descTicket" className="form-label text-light small fw-bold">Descripción del problema</label>
                  <textarea id="descTicket" className="form-control bg-dark text-light border-secondary" rows={3}
                    placeholder="Escribe aquí los detalles de tu requerimiento..." required /></div>
                <div className="mt-4"><button type="submit" className="btn btn-primary btn-lg w-100 fw-bold shadow-sm py-3">
                  <i className="bi bi-send-fill me-2"></i> ENVIAR TICKET DE SOPORTE
                </button></div>
              </form>
            </div>
          </div>
        </div>
      </main>
      <Footer variant="checkout" />
    </PageFrame>
  );
}
