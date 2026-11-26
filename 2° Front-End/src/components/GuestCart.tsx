import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../state/AuthContext';
import { clearGuest, guestItems, removeGuest, setGuest } from '../state/guestCart';
import { clp, errorMessage, storeApi, type Producto } from '../services/storeApi';

/** Revisa precios y stock en Catalog. La importación al carrito real exige confirmación explícita. */
export function GuestCart() {
  const {session}=useAuth();
  const [items,setItems]=useState(guestItems);
  const [products,setProducts]=useState<Record<number,Producto>>({});
  const [busy,setBusy]=useState(false);
  const [message,setMessage]=useState('');
  useEffect(()=>{const change=()=>setItems(guestItems());window.addEventListener('nackson-cart-changed',change);return()=>window.removeEventListener('nackson-cart-changed',change);},[]);
  useEffect(()=>{let active=true;void Promise.allSettled(items.map(i=>storeApi.catalog.detail(i.productId))).then(results=>{if(active)setProducts(Object.fromEntries(results.flatMap(r=>r.status==='fulfilled'?[[r.value.id,r.value]]:[])));});return()=>{active=false;};},[items]);
  const update=async(id:number,qty:number)=>{setBusy(true);setMessage('');try{await setGuest(id,qty);}catch(e){setMessage(errorMessage(e));}finally{setBusy(false);}};
  const transfer=async()=>{
    if(session?.rol!=='CLIENTE'||busy)return;
    setBusy(true);setMessage('');
    try {
      for(const item of guestItems()) {
        const cart=await storeApi.cart.read(session.token);
        const current=cart.items.find(i=>i.productId===item.productId)?.cantidad??0;
        const target=Math.max(current,item.cantidad);
        // Una cantidad absoluta evita duplicar unidades si se reintenta tras una respuesta incierta.
        if(current<target) {
          if(current===0) await storeApi.cart.add(session.token,item.productId,target);
          else await storeApi.cart.set(session.token,item.productId,target);
        }
        removeGuest(item.productId);
      }
      setMessage('Carrito temporal incorporado. Se conservó la mayor cantidad por producto; revisa el resumen real.');
    }catch(e){setMessage('No se completó la incorporación. Los productos pendientes se conservan. '+errorMessage(e));}finally{setBusy(false);}
  };
  if(session?.rol==='ADMINISTRADOR')return null;
  if(!items.length&&!message&&session)return null;
  return <section className="card card-custom p-3 mb-3" aria-label="Carrito temporal">
    <h3 className="h6">Carrito temporal</h3>
    {message&&<p role="status">{message}</p>}
    {items.length===0?<p>No hay productos en el carrito temporal.</p>:<>
      <p className="small">Precios de referencia consultados al catálogo. El servidor valida el precio definitivo al comprar.</p>
      {items.map(item=><div className="border-bottom border-secondary py-2" key={item.productId}>
        <Link to={`/producto/${item.productId}`}>{products[item.productId]?.nombre??`Producto #${item.productId}`}</Link>
        <p className="small mb-1">{products[item.productId]?clp((products[item.productId].precioOferta??products[item.productId].precioBase)*item.cantidad):'Precio sin confirmar'}</p>
        <div className="d-flex align-items-center gap-2"><button type="button" className="btn btn-outline-light btn-sm" disabled={busy||item.cantidad<=1} onClick={()=>void update(item.productId,item.cantidad-1)}>−</button><span>{item.cantidad}</span><button type="button" className="btn btn-outline-light btn-sm" disabled={busy||item.cantidad>=200} onClick={()=>void update(item.productId,item.cantidad+1)}>+</button><button type="button" className="btn btn-outline-danger btn-sm" disabled={busy} onClick={()=>removeGuest(item.productId)}>Quitar</button></div>
      </div>)}
      <div className="d-flex flex-wrap gap-2 mt-3">{session?<button type="button" className="btn btn-primary btn-sm" disabled={busy} onClick={()=>void transfer()}>Incorporar a mi carrito</button>:<Link to="/login.html" state={{from:'/carrito.html'}} className="btn btn-primary btn-sm">Inicia sesión para comprar</Link>}<button type="button" className="btn btn-outline-light btn-sm" disabled={busy} onClick={()=>clearGuest()}>Vaciar temporal</button></div>
      {session&&<p className="small mt-2 mb-0">Si ya tienes el producto, se conserva la mayor cantidad de los dos carritos, para evitar duplicaciones.</p>}
    </>}
  </section>;
}
