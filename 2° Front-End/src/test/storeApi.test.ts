import {afterEach,describe,expect,it,vi} from 'vitest';
import {storeApi,query} from '../services/storeApi';
const ok=(data:unknown={})=>({ok:true,status:200,text:async()=>JSON.stringify(data)});
const mock=()=>{const fn=vi.fn().mockResolvedValue(ok({contenido:[],items:[],metodos:[]}));vi.stubGlobal('fetch',fn);return fn};
afterEach(()=>vi.unstubAllGlobals());
describe('Gateway: rutas y datos reales (sin precios enviados por el cliente)',()=>{
  it('serializa filtros sin valores vacíos',()=>{expect(query({page:0,size:20,q:'anime & rock',genero:''})).toBe('?page=0&size=20&q=anime+%26+rock')});
  const cases:[string,()=>Promise<unknown>,string,string][]=[
    ['catálogo público',()=>storeApi.catalog.list({page:0}),'/api/v1/products?page=0','GET'],
    ['detalle',()=>storeApi.catalog.detail(7),'/api/v1/products/7','GET'],
    ['ofertas',()=>storeApi.catalog.offers(),'/api/v1/offers','GET'],
    ['disponibilidad',()=>storeApi.catalog.availability(2),'/api/v1/inventory/2/availability','GET'],
    ['favoritos',()=>storeApi.catalog.favorites('TOKEN'),'/api/v1/favorites','GET'],
    ['añadir favorito',()=>storeApi.catalog.favoriteAdd('TOKEN',7),'/api/v1/favorites/7','POST'],
    ['quitar favorito',()=>storeApi.catalog.favoriteRemove('TOKEN',7),'/api/v1/favorites/7','DELETE'],
    ['carrito',()=>storeApi.cart.read('TOKEN'),'/api/v1/cart','GET'],
    ['agregar',()=>storeApi.cart.add('TOKEN',7,2),'/api/v1/cart/items','POST'],
    ['editar cantidad',()=>storeApi.cart.set('TOKEN',7,3),'/api/v1/cart/items/7','PATCH'],
    ['quitar producto',()=>storeApi.cart.remove('TOKEN',7),'/api/v1/cart/items/7','DELETE'],
    ['vaciar carrito',()=>storeApi.cart.clear('TOKEN'),'/api/v1/cart','DELETE'],
    ['entregas',()=>storeApi.checkout.deliveries(),'/api/v1/checkout/delivery-methods','GET'],
    ['pagos',()=>storeApi.checkout.payments(),'/api/v1/checkout/payment-methods','GET'],
    ['validar',()=>storeApi.checkout.validate('TOKEN',{metodoEntrega:'RETIRO_TIENDA',nombreReceptor:'Ana Pérez',telefono:'12345678'}),'/api/v1/checkout/validate','POST'],
    ['confirmar',()=>storeApi.checkout.confirm('TOKEN',{datosEntrega:{metodoEntrega:'RETIRO_TIENDA',nombreReceptor:'Ana Pérez',telefono:'12345678'},metodoPago:'WEBPAY',resultadoSimulado:'APROBADO',totalEsperado:9000}),'/api/v1/checkout/confirm','POST'],
    ['mis pedidos',()=>storeApi.orders.mine('TOKEN'),'/api/v1/orders/me','GET'],
    ['seguir pedido',()=>storeApi.orders.tracking('TOKEN','PED-000015'),'/api/v1/orders/me/PED-000015/tracking','GET'],
    ['mis tickets',()=>storeApi.support.mine('TOKEN'),'/api/v1/support/requests/me','GET'],
    ['crear ticket',()=>storeApi.support.create('TOKEN',{asunto:'Problema',categoria:'PEDIDO',mensaje:'Descripción'}),'/api/v1/support/requests','POST'],
    ['responder ticket',()=>storeApi.support.reply('TOKEN',5,'Hola'),'/api/v1/support/requests/5/messages','POST'],
    ['notificaciones',()=>storeApi.notifications.list('TOKEN'),'/api/v1/notifications/me','GET'],
    ['marcar leída',()=>storeApi.notifications.read('TOKEN',8),'/api/v1/notifications/8/read','PATCH'],
    ['cancelación',()=>storeApi.postSale.cancel('TOKEN',15,'No lo quiero'),'/api/v1/post-sale/cancellations','POST'],
    ['devolución',()=>storeApi.postSale.return('TOKEN',15,'Rayado',[{productoId:2,cantidadSolicitada:1}]),'/api/v1/post-sale/returns','POST'],
    ['reembolso',()=>storeApi.postSale.refund('TOKEN',9,'Error'),'/api/v1/post-sale/refund-requests','POST'],
    ['productos admin',()=>storeApi.admin.products('TOKEN'),'/api/v1/admin/products','GET'],
    ['inventario admin',()=>storeApi.admin.inventory('TOKEN'),'/api/v1/admin/inventory','GET'],
    ['movimiento admin',()=>storeApi.admin.movement('TOKEN',3,'SALIDA',1,'venta'),'/api/v1/admin/inventory/3/movements','POST'],
    ['pedidos admin',()=>storeApi.admin.orders('TOKEN'),'/api/v1/admin/orders','GET'],
    ['tickets admin',()=>storeApi.admin.tickets('TOKEN'),'/api/v1/admin/support/requests','GET'],
    ['postventa admin',()=>storeApi.admin.postSale('TOKEN'),'/api/v1/admin/post-sale/requests','GET'],
    ['promos admin',()=>storeApi.admin.promotions('TOKEN'),'/api/v1/admin/promotions','GET'],
  ];
  it.each(cases)('%s',async(_name,fn,path,method)=>{const f=mock();await fn();expect(f).toHaveBeenCalledTimes(1);const [url,options]=f.mock.calls[0];expect(url).toBe(`http://localhost:8080${path}`);expect(options.method).toBe(method);expect(String(url)).not.toContain('/internal/');});
  it('cart add nunca envía userId, precio ni total',async()=>{const f=mock();await storeApi.cart.add('T',5,2);expect(JSON.parse(f.mock.calls[0][1].body)).toEqual({productId:5,cantidad:2});expect(f.mock.calls[0][1].headers.Authorization).toBe('Bearer T');});
  it('confirmación solo envía totalEsperado tras validación',async()=>{const f=mock();await storeApi.checkout.confirm('TOKEN',{datosEntrega:{metodoEntrega:'RETIRO_TIENDA',nombreReceptor:'Ana Pérez',telefono:'12345678'},metodoPago:'WEBPAY',resultadoSimulado:'APROBADO',totalEsperado:9000});const body=JSON.parse(f.mock.calls[0][1].body);expect(body.totalEsperado).toBe(9000);expect(body).not.toHaveProperty('userId');expect(body).not.toHaveProperty('items');expect(body).not.toHaveProperty('precioUnitario');});
  it('retirar usa ids exactos según DTO backend',async()=>{const f=mock();await storeApi.admin.retire('TOKEN',[4,7]);expect(JSON.parse(f.mock.calls[0][1].body)).toEqual({ids:[4,7]})});
  it('PATCH soporte usa parámetro estado en query',async()=>{const f=mock();await storeApi.admin.ticketProgress('TOKEN',3);expect(f.mock.calls[0][0]).toContain('/status?estado=EN_PROCESO');expect(f.mock.calls[0][1].method).toBe('PATCH')});
  it('HTTP 204 sin cuerpo se acepta para notificaciones',async()=>{vi.stubGlobal('fetch',vi.fn().mockResolvedValue({ok:true,status:204,text:async()=>''}));await expect(storeApi.notifications.read('T',3)).resolves.toBeUndefined()});
});
