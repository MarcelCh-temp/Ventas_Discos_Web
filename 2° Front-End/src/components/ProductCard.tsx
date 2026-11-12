import { useState } from 'react';
import { Link } from 'react-router-dom';
import type { CategoryProductPreview, FeaturedProductPreview } from '../models/ProductPreview';
import { getDemoProduct } from '../data/demoCatalog';
import { useCart } from '../state/CartContext';

type ProductCardProps =
  | { variant: 'category'; product: CategoryProductPreview; demoId: string }
  | { variant: 'featured'; product: FeaturedProductPreview };

/** Mantiene las tarjetas Bootstrap originales; el carrito usa solo artículos de DEMOSTRACIÓN. */
export function ProductCard(props: ProductCardProps) {
  if (props.variant === 'featured') return <FeaturedCard product={props.product} />;
  return <CategoryCard product={props.product} demoId={props.demoId} />;
}

function FeaturedCard({ product }: { product: FeaturedProductPreview }) {
  return (
    <div className="col-md-4">
      <div className="card h-100 bg-secondary bg-opacity-10 border border-secondary text-light shadow-sm overflow-hidden">
        <img src={product.imageSrc} alt={product.imageAlt} className="card-img-top object-fit-cover" style={{ height: '220px' }} />
        <div className="card-body d-flex flex-column">
          <h3 className="card-title h5">{product.title}</h3>
          <p className="card-text text-muted small">{product.description}</p>
          <div className="mt-auto">
            <span className="fw-bold text-success fs-5">{product.priceText}</span>
            <Link to={product.linkTo} className="btn btn-outline-primary w-100 mt-2">{product.linkLabel}</Link>
          </div>
        </div>
      </div>
    </div>
  );
}

function CategoryCard({ product, demoId }: { product: CategoryProductPreview; demoId: string }) {
  const { add } = useCart();
  const [added, setAdded] = useState(false);
  function handleAdd() {
    const demo = getDemoProduct(demoId);
    if (!demo) return;
    add(demo);
    setAdded(true);
  }
  return (
    <div className="col-md-6">
      <div className="card h-100 text-light shadow-sm">
        <div className="bg-black text-center py-5 rounded-top border-bottom border-secondary d-flex align-items-center justify-content-center" style={{ height: '180px' }}>
          <i className={product.iconClassName}></i>
        </div>
        <div className="card-body d-flex flex-column">
          <h4 className="card-title h5">{product.title}</h4>
          <p className="card-text text-muted small">{product.description}</p>
          <div className="mt-auto">
            <span className="fw-bold text-success fs-5">{product.priceText}</span>
            <button type="button" className="btn btn-neon-danger w-100 mt-3 py-2 rounded-pill" onClick={handleAdd}>
              Comprar
            </button>
            {added && <p className="text-info small mt-2 mb-0" role="status">
              Añadido al carrito de demostración. <Link to="/carrito.html" className="text-info">Ver carrito</Link>
            </p>}
          </div>
        </div>
      </div>
    </div>
  );
}
