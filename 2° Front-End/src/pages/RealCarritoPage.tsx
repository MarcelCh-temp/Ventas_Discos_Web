import { useCallback, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { StoreLayout } from '../components/StoreLayout';
import { StoreNotice } from '../components/StoreNotice';
import { useAuth } from '../state/AuthContext';
import { storeApi,clp,errorMessage,type Cart } from '../services/storeApi';
export default function RealCarritoPage(){
 const {session}=useAuth();const [cart,setCart]=useState<Cart|null>(null);const [busy,setBusy]=useState(false);const [error,setError]=useState('');
 const refresh=useCallback(async()=>{if(!session)return;setError('');try{setCart(await storeApi.cart.read(session.token));}catch(e){setError(errorMessage(e));}},[session]);
 useEffect(()=>{void refresh();},[refresh]);
 useEffect(()=>{window.addEventListener('nackson-cart-changed',refresh);return()=>window.removeEventListener('nackson-cart-changed',refresh)},[refresh]);
 const act=async(fn:()=>Promise<Cart>)=>{setBusy(true);setError('');try{setCart(await fn());window.dispatchEvent(new Event('nackson-cart-changed'));}catch(e){setError(errorMessage(e));}finally{setBusy(false);}};
 return <StoreLayout title="Tu carrito de compras">
 {!session?<div className="alert alert-info">El carrito de Orders pertenece a tu cuenta. <Link to="/login.html">Inicia sesión</Link> para consultarlo.</div>:<>
 <p className="text-info small">Carrito real administrado por MS-Orders. Precios, promociones y stock vienen del servidor.</p><StoreNotice error={error}/>
 {!cart&&!error&&<p role="status">Cargando carrito de Orders…</p>}
 {cart&&<div className="row g-4"><div className="col-lg-8">
 {cart.items.length===0?<div className="alert alert-secondary text-dark">Tu carrito está vacío. <Link to="/">Ver productos</Link></div>:<div className="table-responsive"><table className="table table-dark table-hover align-middle"><thead><tr><th>Producto</th><th>Precio final</th><th>Cantidad</th><th>Subtotal</th><th>Acción</th></tr></thead><tbody>{cart.items.map(item=><tr key={item.productId}><td><Link to={`/producto/${item.productId}`} className="text-info">{item.nombre}</Link>{item.motivo&&<div className="text-warning small">{item.motivo}</div>}</td><td>{clp(item.precioFinalUnitario)}</td><td><div className="d-flex gap-2 align-items-center"><button type="button" className="btn btn-outline-light btn-sm" disabled={busy||item.cantidad<=1} aria-label={`Disminuir ${item.nombre}`} onClick={()=>void act(()=>storeApi.cart.set(session.token,item.productId,item.cantidad-1))}>−</button><span>{item.cantidad}</span><button type="button" className="btn btn-outline-light btn-sm" disabled={busy} aria-label={`Aumentar ${item.nombre}`} onClick={()=>void act(()=>storeApi.cart.set(session.token,item.productId,item.cantidad+1))}>+</button></div></td><td>{clp(item.subtotal)}</td><td><button type="button" className="btn btn-outline-danger btn-sm" disabled={busy} onClick={()=>void act(()=>storeApi.cart.remove(session.token,item.productId))}>Quitar</button></td></tr>)}</tbody></table><button type="button" className="btn btn-outline-danger" disabled={busy} onClick={()=>{if(window.confirm('¿Vaciar todo el carrito?'))void act(()=>storeApi.cart.clear(session.token));}}>Vaciar carrito</button></div>}
 </div><div className="col-lg-4"><div className="card card-custom p-4 text-light"><h3 className="h5">Resumen del servidor</h3><p>Unidades: {cart.unidades}</p><p>Subtotal: {clp(cart.subtotal)}</p><p>Descuento: {clp(cart.descuento)}</p><h4 className="text-success">Total: {clp(cart.total)}</h4>{cart.avisos?.map((a,i)=><p key={i} className="text-warning small">{a}</p>)}{!cart.preciosActualizados&&<p className="text-warning">No se pudieron actualizar los precios. No se puede comprar todavía.</p>}{cart.listoParaComprar&&cart.preciosActualizados?<Link className="btn btn-success" to="/checkout.html">Revisar checkout</Link>:<button className="btn btn-success" disabled>Checkout no disponible</button>}<button type="button" className="btn btn-outline-light mt-2" onClick={()=>void refresh()} disabled={busy}>Actualizar carrito</button></div></div></div>}
 </>}
 </StoreLayout>
}
