import type { ReactNode } from 'react';
import { Link } from 'react-router-dom';
import { PageFrame } from './PageFrame';
import { Navbar } from './Navbar';
import { Footer } from './Footer';
import css from '../styles/checkout.css?inline';
export function StoreLayout({title,children}:{title:string;children:ReactNode}) {
 return <PageFrame title={`${title} - Ñackson Store`} css={css} bodyClass="d-flex flex-column min-vh-100 bg-dark text-light">
  <Navbar variant="checkout" />
  <main className="container my-5 flex-grow-1"><h1 className="h2 text-light mb-3">{title}</h1>{children}</main>
  <div className="container pb-4 d-flex flex-wrap gap-3 small"><Link to="/" className="text-info">Inicio</Link><Link to="/carrito.html" className="text-info">Carrito</Link><Link to="/pedidos" className="text-info">Mis pedidos</Link><Link to="/soporte" className="text-info">Soporte</Link><Link to="/postventa" className="text-info">Postventa</Link><Link to="/notificaciones" className="text-info">Notificaciones</Link><Link to="/favoritos" className="text-info">Favoritos</Link></div>
  <Footer variant="checkout" />
 </PageFrame>;
}
