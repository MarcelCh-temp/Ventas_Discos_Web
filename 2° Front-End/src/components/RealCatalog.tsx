import { useCallback, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../state/AuthContext';
import { storeApi, clp, errorMessage, type Categoria, type Producto } from '../services/storeApi';
import type { Params } from '../services/storeApi';

type Props = { categoria?: Categoria; genero?: string; formato?: string; q?: string; titulo?: string; limit?: number; mensajeCategoria?: string };
/** Catálogo de la API real. Ningún artículo ficticio; la fuente de verdad es Catalog. */
export function RealCatalog({categoria,genero,formato,q,titulo,limit,mensajeCategoria}: Props) {
  const auth = useAuth();
  const [items,setItems] = useState<Producto[]>([]);
  const [page,setPage] = useState(0);
  const [total,setTotal] = useState(0);
  const [loading,setLoading] = useState(true);
  const [failure,setFailure] = useState('');
  const [notice,setNotice] = useState('');
  const [busy,setBusy] = useState<number|null>(null);
  const [filters,setFilters] = useState({precioMin:'',precioMax:'',disponibilidad:''});
  const [applied,setApplied] = useState(filters);
  const reload=useCallback(async () => {
    const params: Params = {page,size:limit||12,categoria,genero,formato,q,
      precioMin:applied.precioMin,precioMax:applied.precioMax,disponibilidad:applied.disponibilidad};
    setLoading(true);setFailure('');
    try {const data=await storeApi.catalog.list(params);setItems(data.contenido);setTotal(data.totalPaginas);}
    catch(e){setItems([]);setFailure(errorMessage(e));}
    finally{setLoading(false);}
  },[page,limit,categoria,genero,formato,q,applied]);
  useEffect(()=>{void reload();},[reload]);
  const add=async(id:number)=>{
    if (!auth.session){setNotice('Inicia sesión para añadir productos al carrito de Orders.');return;}
    setBusy(id);setNotice('');
    try{await storeApi.cart.add(auth.session.token,id,1);setNotice('Producto añadido al carrito real.');window.dispatchEvent(new Event('nackson-cart-changed'));}
    catch(e){setNotice(errorMessage(e));}finally{setBusy(null);}
  };
  return <section aria-label={titulo||'Catálogo de productos'} className="w-100">
    {titulo && <h2 className="h4 text-light mb-3">{titulo}</h2>}
    {mensajeCategoria && <p className="small text-info">{mensajeCategoria}</p>}
    <form data-nackson-react-form className="row g-2 mb-3" onSubmit={e=>{e.preventDefault();setPage(0);setApplied({...filters});}}>
      <div className="col-sm-3"><input aria-label="Precio mínimo" type="number" min="0" step="1" value={filters.precioMin} placeholder="Precio mínimo" className="form-control bg-dark text-light border-secondary" onChange={e=>setFilters({...filters,precioMin:e.target.value})}/></div>
      <div className="col-sm-3"><input aria-label="Precio máximo" type="number" min="0" step="1" value={filters.precioMax} placeholder="Precio máximo" className="form-control bg-dark text-light border-secondary" onChange={e=>setFilters({...filters,precioMax:e.target.value})}/></div>
      <div className="col-sm-4"><select aria-label="Disponibilidad" className="form-select bg-dark text-light border-secondary" value={filters.disponibilidad} onChange={e=>setFilters({...filters,disponibilidad:e.target.value})}><option value="">Cualquier disponibilidad</option><option value="DISPONIBLE">Disponible</option><option value="STOCK_CRITICO">Stock crítico</option><option value="AGOTADO">Agotado</option></select></div>
      <div className="col-sm-2"><button type="submit" className="btn btn-outline-danger w-100">Filtrar</button></div>
    </form>
    {loading && <p role="status" className="text-light">Cargando productos desde Catalog…</p>}
    {failure && <div role="alert" className="alert alert-warning">{failure} <button type="button" onClick={()=>void reload()} className="btn btn-outline-dark btn-sm ms-2">Reintentar</button></div>}
    {notice && <p role="status" className="alert alert-info">{notice}</p>}
    {!loading&&!failure&&items.length===0&&<p className="text-light">No hay productos que coincidan con los filtros en Catalog.</p>}
    <div className="row g-4">
      {items.map(product=><div key={product.id} className="col-md-6 col-xl-4">
        <div className="card bg-dark border-secondary h-100 text-light shadow-sm">
          {product.imagenUrl && /^https?:\/\//.test(product.imagenUrl)?<img src={product.imagenUrl} alt={product.nombre} className="card-img-top object-fit-contain bg-black" style={{height:190}}/>:<div className="bg-black text-center d-flex justify-content-center align-items-center" style={{height:190}}><i className="bi bi-disc fs-1 text-danger" /></div>}
          <div className="card-body d-flex flex-column">
            <h3 className="h5">{product.nombre}</h3>
            <p className="text-white-50 small">{product.genero} · {product.formato}</p>
            <p className="small text-white-50 flex-grow-1">{product.descripcion}</p>
            <div className="fw-bold fs-5 text-success">{clp(product.precioOferta??product.precioBase)}</div>
            {product.precioOferta!==null&&<div className="text-white-50 small"><s>{clp(product.precioBase)}</s> · {product.promocion}</div>}
            <p className="small text-info mb-2">{product.disponibilidad.replace('_',' ')}</p>
            <div className="d-flex gap-2"><Link className="btn btn-outline-light btn-sm flex-fill" to={`/producto/${product.id}`}>Ver detalle</Link><button type="button" className="btn btn-danger btn-sm flex-fill" disabled={!product.comprable||busy===product.id} onClick={()=>void add(product.id)}>{busy===product.id?'Añadiendo…':'Agregar'}</button></div>
          </div>
        </div>
      </div>)}
    </div>
    {!loading&&total>1&&<div className="d-flex justify-content-center align-items-center gap-3 mt-4"><button type="button" className="btn btn-outline-light" disabled={page===0} onClick={()=>setPage(p=>p-1)}>Anterior</button><span className="text-light">Página {page+1} de {total}</span><button type="button" className="btn btn-outline-light" disabled={page+1>=total} onClick={()=>setPage(p=>p+1)}>Siguiente</button></div>}
  </section>;
}
