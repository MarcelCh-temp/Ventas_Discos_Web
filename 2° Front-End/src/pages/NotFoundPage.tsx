import { Link } from 'react-router-dom';
export default function NotFoundPage() {
  return <main className="container py-5 text-light text-center"><h1>404</h1><p>Esta página no existe.</p><Link className="btn btn-primary" to="/">Volver a Ñackson Store</Link></main>;
}
