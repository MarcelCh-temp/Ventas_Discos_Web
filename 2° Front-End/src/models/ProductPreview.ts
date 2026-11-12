/** Modelos de visualización para ejemplos estáticos del frontend antiguo. NO son DTO de Catalog. */
export type CategoryProductPreview = Readonly<{
  title: string;
  description: string;
  priceText: string;
  iconClassName: string;
  legacyAction: string;
}>;
export type FeaturedProductPreview = Readonly<{
  title: string;
  description: string;
  priceText: string;
  imageSrc: string;
  imageAlt: string;
  linkTo: string;
  linkLabel: string;
}>;
