import { ProductCard } from './ProductCard';
import type { CategoryProductPreview, FeaturedProductPreview } from '../models/ProductPreview';
import type { DemoCategory } from '../data/demoCatalog';

type ProductListProps =
  | { variant: 'category'; category: DemoCategory; products: readonly CategoryProductPreview[] }
  | { variant: 'featured'; products: readonly FeaturedProductPreview[] };

export function ProductList(props: ProductListProps) {
  if (props.variant === 'featured') {
    return <>{props.products.map(product => <ProductCard key={`${product.title}:${product.linkTo}`} variant="featured" product={product} />)}</>;
  }
  return <>{props.products.map((product, index) =>
    <ProductCard key={`${props.category}:${index}`} variant="category" product={product}
      demoId={`demo:${props.category}:${index}`} />
  )}</>;
}
