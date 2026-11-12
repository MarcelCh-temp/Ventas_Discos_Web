/** Contratos públicos del Gateway. Lotes 1–6; no usar rutas internas ni precios enviados por el navegador. */
import { http } from './http';
export type Params = Record<string, string | number | boolean | null | undefined>;
export function query(params: Params = {}): string {
  const q = new URLSearchParams();
  for (const [key, value] of Object.entries(params)) if (value !== null && value !== undefined && String(value) !== '') q.set(key, String(value));
  const result = q.toString(); return result ? `?${result}` : '';
}
const get = <T>(path: string, token?: string) => http<T>(path, { token });
const send = <T>(path: string, method: 'POST' | 'PUT' | 'PATCH' | 'DELETE', token?: string, body?: unknown) => http<T>(path, { method, token, body });
export interface Page<T> { contenido: T[]; pagina: number; tamano: number; totalElementos: number; totalPaginas: number }
export type Categoria = 'MUSICA' | 'SERIES_PELICULAS_ANIME' | 'VIDEOJUEGOS';
export type Disponibilidad = 'AGOTADO' | 'STOCK_CRITICO' | 'DISPONIBLE' | 'NO_CONFIRMADA';
export interface Producto { id: number; nombre: string; descripcion: string | null; categoria: Categoria; genero: string; formato: string; plataforma: string | null; imagenUrl: string | null; precioBase: number; precioOferta: number | null; promocion: string | null; disponibilidad: Disponibilidad; comprable: boolean }
export interface ProductoAdmin extends Producto { estado: string; stockActual: number | null }
export interface ProductoRequest { nombre: string; descripcion: string; precioBase: number; categoria: Categoria; genero: string; formato: string; plataforma: string; imagenUrl: string | null }
export interface Favorito { productId: number; nombre: string; imagenUrl: string | null; precioBase: number; estadoComercial: string; disponibilidad: Disponibilidad; comprable: boolean; mensaje: string | null }
export interface Oferta { promocionId: number; nombre: string; tipoDescuento: 'PORCENTAJE' | 'MONTO'; valor: number; montoMinimoCompra: number | null; fechaFin: string; productos: {id:number; nombre:string; precioBase:number; precioOferta:number; imagenUrl:string|null}[] }
export interface Inventario {productId:number; nombre:string; categoria:Categoria; stockActual:number; umbralCritico:number; disponibilidad:Disponibilidad}
export interface Movimiento {id:number; productId:number; tipo:'ENTRADA'|'SALIDA'|'AJUSTE'; cantidad:number; stockAnterior:number; stockResultante:number; fechaHora:string; motivo:string|null; usuarioId:number|null; operacionId:string|null}
export interface ItemCart { productId:number; nombre:string; cantidad:number; precioUnitario:number; descuentoUnitario:number; precioFinalUnitario:number; subtotal:number; disponibilidad:Disponibilidad; haySuficiente:boolean; comprable:boolean; motivo:string|null }
export interface Cart {items:ItemCart[]; cantidadProductos:number; unidades:number; subtotal:number; descuento:number; total:number; vacio:boolean; listoParaComprar:boolean; preciosActualizados:boolean; avisos:string[]}
export interface MetodoEntrega { codigo:string; nombre:string; camposObligatorios:string[]; camposOpcionales:string[] }
export interface OpcionesPago {simulacionAcademica:boolean; aviso:string; metodos:{codigo:string;nombre:string}[]; resultadosSimulados:string[]; resultadoPorDefecto:string}
export interface DatosEntrega { metodoEntrega:string; nombreReceptor:string; telefono:string; direccion?:string; comuna?:string; referencia?:string }
export interface ResumenCompra {items:ItemCart[]; subtotal:number; descuento:number; total:number; datosEntrega:DatosEntrega & {nombreMetodo?:string}; listo:boolean; problemas: Record<string,string>}
export interface PedidoResumen {id:number; numeroPedido:string|null; userId:number; fechaCompra:string; total:number; estado:string; metodoEntrega:string; nombreReceptor:string; cantidadProductos:number}
export interface PedidoDetalle extends PedidoResumen {items:{productId:number; nombreProducto:string; precioUnitario:number; cantidad:number; subtotal:number}[];subtotal:number;descuento:number; datosEntrega: DatosEntrega|null; despacho:{fechaDespacho:string; transportista:string;numeroSeguimiento:string|null}|null; historial:{fechaHora:string; estadoAnterior:string|null;estadoNuevo:string;motivo:string;descripcion:string;usuarioResponsableId:number|null}[]}
export interface ConfirmacionCompra {mensaje:string; pago:{metodo:string;resultado:string;simulacionAcademica:boolean};pedido:PedidoDetalle}
export interface Seguimiento {id:number;numeroPedido:string;estado:string;mensaje:string;cancelado:boolean;finalizado:boolean;pasos:{estado:string;completado:boolean;actual:boolean}[];despacho:PedidoDetalle['despacho'];historial:PedidoDetalle['historial']}
export interface TicketResumen {id:number;numeroTicket:string|null;idUsuario:number;orderId:number|null;asunto:string;categoria:string;estado:'ABIERTA'|'EN_PROCESO'|'CERRADA';fechaCreacion:string;fechaCierre:string|null}
export interface TicketDetalle extends TicketResumen {mensajes:{id:number;autor:'CLIENTE'|'ADMINISTRADOR';autorId:number;contenido:string;fechaEnvio:string}[]; historial:{estadoAnterior:string|null;estadoNuevo:string;fechaHora:string;usuarioResponsableId:number|null}[]}
export interface Notificacion {id:number;usuarioId:number;titulo:string;mensaje:string;tipo:string;fechaCreacion:string;leida:boolean;referenciaId:number|null}
export interface SolicitudResumen {id:number;tipo:'CANCELACION'|'DEVOLUCION'|'REEMBOLSO';estado:string;pedidoId:number;solicitudOrigenId:number|null;fechaSolicitud:string;fechaResolucion:string|null}
export interface SolicitudDetalle extends SolicitudResumen {motivoSolicitud?:string;motivoResolucion?:string;items?:{id:number;productoId:number;cantidadSolicitada:number;cantidadRecibida:number}[];recepciones?:unknown[];usuarioResolucionId?:number|null}
export interface Promocion {id:number;nombre:string;tipoDescuento:'PORCENTAJE'|'MONTO';valor:number;fechaInicio:string;fechaFin:string;montoMinimoCompra:number|null;productIds:number[];categoria:Categoria|null;estado:string}
export const storeApi = {
  catalog: {
    list:(params:Params={})=>get<Page<Producto>>('/api/v1/products'+query(params)),
    detail:(id:number)=>get<Producto>(`/api/v1/products/${id}`),
    offers:()=>get<Oferta[]>('/api/v1/offers'),
    availability:(id:number)=>get<{productId:number;disponibilidad:Disponibilidad;permiteCompra:boolean}>(`/api/v1/inventory/${id}/availability`),
    availabilityMany:(items:{productId:number;cantidad:number}[])=>send<{productId:number;cantidadSolicitada:number;disponibilidad:Disponibilidad;haySuficiente:boolean;permiteCompra:boolean}[]>('/api/v1/inventory/availability/query','POST',undefined,{items}),
    favorites:(token:string)=>get<Favorito[]>('/api/v1/favorites',token),
    favoriteAdd:(token:string,id:number)=>send<Favorito>(`/api/v1/favorites/${id}`,'POST',token),
    favoriteRemove:(token:string,id:number)=>send<void>(`/api/v1/favorites/${id}`,'DELETE',token),
  },
  cart: {
    read:(token:string)=>get<Cart>('/api/v1/cart',token),
    add:(token:string,productId:number,cantidad:number)=>send<Cart>('/api/v1/cart/items','POST',token,{productId,cantidad}),
    set:(token:string,productId:number,cantidad:number)=>send<Cart>(`/api/v1/cart/items/${productId}`,'PATCH',token,{cantidad}),
    remove:(token:string,productId:number)=>send<Cart>(`/api/v1/cart/items/${productId}`,'DELETE',token),
    clear:(token:string)=>send<Cart>('/api/v1/cart','DELETE',token),
  },
  checkout: {
    deliveries:()=>get<MetodoEntrega[]>('/api/v1/checkout/delivery-methods'),
    payments:()=>get<OpcionesPago>('/api/v1/checkout/payment-methods'),
    validate:(token:string,datosEntrega:DatosEntrega)=>send<ResumenCompra>('/api/v1/checkout/validate','POST',token,{datosEntrega}),
    confirm:(token:string,body:{datosEntrega:DatosEntrega;metodoPago:string;resultadoSimulado:'APROBADO'|'RECHAZADO';totalEsperado:number})=>send<ConfirmacionCompra>('/api/v1/checkout/confirm','POST',token,body),
  },
  orders: {
    mine:(token:string,params:Params={})=>get<Page<PedidoResumen>>('/api/v1/orders/me'+query(params),token),
    detail:(token:string,id:number)=>get<PedidoDetalle>(`/api/v1/orders/me/${id}`,token),
    tracking:(token:string,ref:string)=>get<Seguimiento>(`/api/v1/orders/me/${encodeURIComponent(ref)}/tracking`,token),
  },
  support: {
    mine:(token:string)=>get<TicketResumen[]>('/api/v1/support/requests/me',token),
    detail:(token:string,id:number)=>get<TicketDetalle>(`/api/v1/support/requests/${id}`,token),
    create:(token:string,body:{asunto:string;categoria:string;mensaje:string;orderId?:number})=>send<TicketDetalle>('/api/v1/support/requests','POST',token,body),
    reply:(token:string,id:number,contenido:string)=>send<TicketDetalle>(`/api/v1/support/requests/${id}/messages`,'POST',token,{contenido}),
  },
  notifications: {
    list:(token:string,params:Params={})=>get<Notificacion[]>('/api/v1/notifications/me'+query(params),token),
    unread:(token:string)=>get<{unreadCount:number}>('/api/v1/notifications/me/unread-count',token),
    read:(token:string,id:number)=>send<void>(`/api/v1/notifications/${id}/read`,'PATCH',token),
    allRead:(token:string)=>send<{updatedCount:number}>('/api/v1/notifications/read-all','PATCH',token),
  },
  postSale: {
    mine:(token:string,params:Params={})=>get<Page<SolicitudResumen>>('/api/v1/post-sale/requests/me'+query(params),token),
    detail:(token:string,id:number)=>get<SolicitudDetalle>(`/api/v1/post-sale/requests/${id}`,token),
    cancel:(token:string,pedidoId:number,motivoSolicitud:string)=>send<SolicitudDetalle>('/api/v1/post-sale/cancellations','POST',token,{pedidoId,motivoSolicitud}),
    return:(token:string,pedidoId:number,motivoSolicitud:string,items:{productoId:number;cantidadSolicitada:number}[])=>send<SolicitudDetalle>('/api/v1/post-sale/returns','POST',token,{pedidoId,motivoSolicitud,items}),
    refund:(token:string,solicitudOrigenId:number,motivoSolicitud:string)=>send<SolicitudDetalle>('/api/v1/post-sale/refund-requests','POST',token,{solicitudOrigenId,motivoSolicitud}),
  },
  admin: {
    stats:(token:string)=>get<{totalUsuarios:number;totalClientes:number}>('/api/v1/accounts/stats',token),
    users:(token:string,ids:number[])=>get<unknown>('/api/v1/admin/accounts'+query({ids:ids.join(',')}),token),
    products:(token:string,params:Params={})=>get<Page<ProductoAdmin>>('/api/v1/admin/products'+query(params),token),
    product:(token:string,id:number)=>get<ProductoAdmin>(`/api/v1/admin/products/${id}`,token),
    saveProduct:(token:string,body:ProductoRequest,id?:number)=>send<ProductoAdmin>(id?`/api/v1/admin/products/${id}`:'/api/v1/admin/products',id?'PUT':'POST',token,body),
    retire:(token:string,ids:number[])=>send<unknown>('/api/v1/admin/products/retire','POST',token,{ids}),
    inventory:(token:string,params:Params={})=>get<Page<Inventario>>('/api/v1/admin/inventory'+query(params),token),
    movements:(token:string,params:Params={})=>get<Page<Movimiento>>('/api/v1/admin/inventory/movements'+query(params),token),
    movement:(token:string,id:number,tipo:'ENTRADA'|'SALIDA',cantidad:number,motivo:string)=>send<Movimiento>(`/api/v1/admin/inventory/${id}/movements`,'POST',token,{tipo,cantidad,motivo}),
    adjustment:(token:string,id:number,nuevoStock:number,motivo:string)=>send<Movimiento>(`/api/v1/admin/inventory/${id}/adjustments`,'POST',token,{nuevoStock,motivo}),
    critical:(token:string,id:number,umbralCritico:number)=>send<Inventario>(`/api/v1/admin/inventory/${id}/critical-level`,'PUT',token,{umbralCritico}),
    bulk:(token:string,operaciones:unknown[])=>send<Movimiento[]>('/api/v1/admin/inventory/bulk','POST',token,{operaciones}),
    orders:(token:string,params:Params={})=>get<Page<PedidoResumen>>('/api/v1/admin/orders'+query(params),token),
    order:(token:string,id:number)=>get<PedidoDetalle>(`/api/v1/admin/orders/${id}`,token),
    orderStatus:(token:string,id:number,estado:string,motivo:string)=>send<PedidoDetalle>(`/api/v1/admin/orders/${id}/status`,'PATCH',token,{estado,motivo}),
    delivery:(token:string,id:number,body:Record<string,string>)=>send<PedidoDetalle>(`/api/v1/admin/orders/${id}/delivery-data`,'PATCH',token,body),
    dispatch:(token:string,id:number,body:{transportista:string;numeroSeguimiento?:string;fechaDespacho?:string})=>send<PedidoDetalle>(`/api/v1/admin/orders/${id}/dispatch`,'POST',token,body),
    tickets:(token:string,params:Params={})=>get<Page<TicketResumen>>('/api/v1/admin/support/requests'+query(params),token),
    ticket:(token:string,id:number)=>get<TicketDetalle>(`/api/v1/admin/support/requests/${id}`,token),
    ticketReply:(token:string,id:number,contenido:string)=>send<TicketDetalle>(`/api/v1/admin/support/requests/${id}/messages`,'POST',token,{contenido}),
    ticketProgress:(token:string,id:number)=>send<TicketDetalle>(`/api/v1/admin/support/requests/${id}/status?estado=EN_PROCESO`,'PATCH',token),
    ticketClose:(token:string,id:number)=>send<TicketDetalle>(`/api/v1/admin/support/requests/${id}/close`,'POST',token),
    postSale:(token:string,params:Params={})=>get<Page<SolicitudDetalle>>('/api/v1/admin/post-sale/requests'+query(params),token),
    resolveCancel:(token:string,id:number,aprobar:boolean,motivo:string)=>send<{solicitud:SolicitudDetalle;estadoPedido:string;reposicionStockPendiente:boolean|null}>(`/api/v1/admin/post-sale/cancellations/${id}/resolve`,'POST',token,{aprobar,motivo}),
    resolveReturn:(token:string,id:number,aprobar:boolean,motivo:string)=>send<SolicitudDetalle>(`/api/v1/admin/post-sale/returns/${id}/resolve`,'POST',token,{aprobar,motivo}),
    reception:(token:string,id:number,body:{observaciones:string;items:{itemDevolucionId:number;cantidadRecibida:number;condicion:'BUENA'|'DANADA'}[]})=>send<SolicitudDetalle>(`/api/v1/admin/post-sale/returns/${id}/reception`,'POST',token,body),
    promotions:(token:string,params:Params={})=>get<Page<Promocion>>('/api/v1/admin/promotions'+query(params),token),
    promotion:(token:string,id:number)=>get<Promocion>(`/api/v1/admin/promotions/${id}`,token),
    savePromotion:(token:string,body:unknown,id?:number)=>send<Promocion>(id?`/api/v1/admin/promotions/${id}`:'/api/v1/admin/promotions',id?'PUT':'POST',token,body),
    deactivatePromotion:(token:string,id:number)=>send<Promocion>(`/api/v1/admin/promotions/${id}/deactivate`,'PATCH',token),
  }
};
export const clp = (n:number)=>new Intl.NumberFormat('es-CL',{style:'currency',currency:'CLP',maximumFractionDigits:0}).format(n);
export function errorMessage(error:unknown) {return error instanceof Error ? error.message : 'No se pudo completar la operación.'}
