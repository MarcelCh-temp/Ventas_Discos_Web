import { createContext, useCallback, useContext, useEffect, useRef, useState, type ReactNode } from 'react';
import { Link, useLocation } from 'react-router-dom';
import { useAuth } from '../state/AuthContext';
import { storeApi, clp, errorMessage, type Cart } from '../services/storeApi';
import '../styles/carrito-lateral.css';

type DrawerState = { open: () => void; close: () => void; units: number | null };
const DrawerContext = createContext<DrawerState | null>(null);

export function CartDrawerProvider({ children }: { children: ReactNode }) {
  const { session } = useAuth();
  const { pathname } = useLocation();
  const [isOpen, setIsOpen] = useState(false);
  const [cart, setCart] = useState<Cart | null>(null);
  const [loading, setLoading] = useState(false);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const [units, setUnits] = useState<number | null>(null);
  const closeButton = useRef<HTMLButtonElement>(null);
  const returnFocus = useRef<HTMLElement | null>(null);
  const requestSeq = useRef(0);
  const isOpenRef = useRef(false);
  isOpenRef.current = isOpen;

  const close = useCallback(() => setIsOpen(false), []);
  const open = useCallback(() => { returnFocus.current = document.activeElement instanceof HTMLElement ? document.activeElement : null; setIsOpen(true); }, []);

  const load = useCallback(async (visible: boolean) => {
    const seq = ++requestSeq.current;
    if (!session) { setCart(null); setUnits(null); setError(''); return; }
    if (visible) { setLoading(true); setError(''); }
    try {
      const next = await storeApi.cart.read(session.token);
      if (seq === requestSeq.current) { setCart(next); setUnits(next.unidades); }
    } catch (cause) {
      if (seq === requestSeq.current && visible) setError(errorMessage(cause));
    } finally {
      if (seq === requestSeq.current && visible) setLoading(false);
    }
  }, [session?.token]);

  useEffect(() => { if (isOpen) void load(true); }, [isOpen, load]);
  useEffect(() => {
    const handler = () => { if (!busy) void load(isOpenRef.current); };
    window.addEventListener('nackson-cart-changed', handler);
    return () => window.removeEventListener('nackson-cart-changed', handler);
  }, [load, busy]);
  useEffect(() => { ++requestSeq.current; setCart(null); setUnits(null); setIsOpen(false); }, [session?.userId, session?.token]);
  useEffect(() => { setIsOpen(false); }, [pathname]);

  useEffect(() => {
    if (!isOpen) return;
    const originalOverflow = document.body.style.overflow;
    document.body.style.overflow = 'hidden';
    closeButton.current?.focus();
    const keyHandler = (event: KeyboardEvent) => {
      if (event.key === 'Escape') { event.preventDefault(); close(); }
      if (event.key !== 'Tab') return;
      const dialog = document.querySelector<HTMLElement>('[data-nackson-cart-drawer]');
      if (!dialog) return;
      const interactive = Array.from(dialog.querySelectorAll<HTMLElement>('a[href],button:not([disabled])'));
      if (!interactive.length) return;
      const first = interactive[0];
      const last = interactive[interactive.length - 1];
      if (event.shiftKey && document.activeElement === first) { event.preventDefault(); last.focus(); }
      else if (!event.shiftKey && document.activeElement === last) { event.preventDefault(); first.focus(); }
    };
    window.addEventListener('keydown', keyHandler);
    return () => {
      document.body.style.overflow = originalOverflow;
      window.removeEventListener('keydown', keyHandler);
      returnFocus.current?.focus();
    };
  }, [isOpen, close]);

  const change = async (productId: number, quantity: number, kind: 'set' | 'remove') => {
    if (!session || busy) return;
    setBusy(true); setError('');
    try {
      if (kind === 'set') {
        const latest = await storeApi.catalog.detail(productId);
        if (latest.stockActual != null && quantity > latest.stockActual) {
          setError(`No hay más unidades disponibles. Stock: ${latest.stockActual}. Ya tienes ${quantity - 1} en el carrito.`);
          return;
        }
        if (!latest.comprable) { setError('Este producto ya no está disponible.'); return; }
      }
      const next = kind === 'remove'
        ? await storeApi.cart.remove(session.token, productId)
        : await storeApi.cart.set(session.token, productId, quantity);
      ++requestSeq.current;
      setCart(next); setUnits(next.unidades);
      window.dispatchEvent(new Event('nackson-cart-changed'));
    } catch (cause) { setError(errorMessage(cause)); }
    finally { setBusy(false); }
  };

  return <DrawerContext.Provider value={{ open, close, units }}>
    {children}
    {isOpen && <div className="nackson-cart-layer">
      <div className="nackson-cart-shade" onClick={close} aria-hidden="true" />
      <aside data-nackson-cart-drawer className="nackson-cart-panel" role="dialog" aria-modal="true" aria-label="Mi carrito de compras">
        <header className="nackson-cart-header">
          <div><span className="nackson-cart-brand">ÑACKSON STORE</span><h2>Mi carrito <span>{units == null ? '' : `(${units})`}</span></h2></div>
          <button ref={closeButton} type="button" className="nackson-cart-close" aria-label="Cerrar carrito" onClick={close}>×</button>
        </header>
        <div className="nackson-cart-content">
          {!session ? <div className="nackson-cart-empty"><p>Inicia sesión para consultar tu carrito real.</p><Link to="/login.html" onClick={close}>Iniciar sesión</Link></div> : <>
            {loading && !cart && <p role="status">Cargando carrito…</p>}
            {error && <div role="alert" className="nackson-cart-error">{error}</div>}
            {cart?.items.length === 0 && <div className="nackson-cart-empty"><i className="bi bi-basket3" aria-hidden="true" /><p>Tu carrito está vacío.</p><Link to="/resultados.html" onClick={close}>Explorar productos</Link></div>}
            {cart?.items.map(item => <div className="nackson-cart-item" key={item.productId}>
              <Link to={`/producto/${item.productId}`} onClick={close} className="nackson-cart-item-name">{item.nombre}</Link>
              <div className="nackson-cart-item-info"><span>{clp(item.precioFinalUnitario)} c/u</span><strong>{clp(item.subtotal)}</strong></div>
              <div className="nackson-cart-item-actions">
                <div className="nackson-cart-quantity">
                  <button type="button" disabled={busy || item.cantidad <= 1} aria-label={`Disminuir ${item.nombre}`} onClick={() => void change(item.productId, item.cantidad - 1, 'set')}>−</button>
                  <span>{item.cantidad}</span>
                  <button type="button" disabled={busy} aria-label={`Aumentar ${item.nombre}`} onClick={() => void change(item.productId, item.cantidad + 1, 'set')}>+</button>
                </div>
                <button type="button" className="nackson-cart-remove" disabled={busy} onClick={() => void change(item.productId, 0, 'remove')}>Quitar</button>
              </div>
              {item.motivo && <p className="nackson-cart-warning">{item.motivo}</p>}
            </div>)}
            {cart?.avisos?.map((message, i) => <p className="nackson-cart-warning" key={i}>{message}</p>)}
          </>}
        </div>
        <footer className="nackson-cart-footer">
          {session && cart && <>
            <div><span>Subtotal</span><span>{clp(cart.subtotal)}</span></div>
            <div><span>Descuentos</span><span>{clp(cart.descuento)}</span></div>
            <div className="nackson-cart-total"><strong>Total</strong><strong>{clp(cart.total)}</strong></div>
          </>}
          <Link className="nackson-cart-primary" to="/carrito.html" onClick={close}>Ver carrito completo</Link>
          {session && cart?.listoParaComprar && cart.preciosActualizados &&
            <Link className="nackson-cart-checkout" to="/checkout.html" onClick={close}>Continuar al checkout</Link>}
        </footer>
      </aside>
    </div>}
  </DrawerContext.Provider>;
}

export function CartDrawerTrigger() {
  const drawer = useContext(DrawerContext);
  if (!drawer) throw new Error('CartDrawerTrigger requiere CartDrawerProvider.');
  return <button type="button" className="nackson-cart-trigger nav-link text-light" aria-label="Abrir carrito lateral" onClick={drawer.open}>
    <i className="bi bi-cart-fill" aria-hidden="true" /> <span>Carrito</span>
    {drawer.units != null && drawer.units > 0 && <span className="nackson-cart-count">{drawer.units}</span>}
  </button>;
}
