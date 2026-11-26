import {Link} from 'react-router-dom';
import {StoreLayout} from '../components/StoreLayout';
export default function NotFoundPage(){return <StoreLayout title="Página no encontrada"><section className="nx-error-page nx-glass"><i className="bi bi-disc"/><h2>404</h2><p>Esta página no existe.</p><Link className="nx-cta" to="/">Volver a Ñackson Store</Link></section></StoreLayout>;}
