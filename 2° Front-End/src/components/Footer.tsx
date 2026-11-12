/** Footers de las vistas originales con variantes tipadas, sin cambiar estilos. */
export type FooterVariant = 'category' | 'home' | 'accessories' | 'admin' | 'cart' | 'checkout' | 'results';
export function Footer({ variant }: { variant: FooterVariant }) {
  switch (variant) {
    case 'category': return <CategoryFooter />;
    case 'home': return <HomeFooter />;
    case 'accessories': return <AccessoriesFooter />;
    case 'admin': return <AdminFooter />;
    case 'cart': return <CartFooter />;
    case 'checkout': return <CheckoutFooter />;
    case 'results': return <ResultsFooter />;
  }
}

function CategoryFooter() {
  return (
<footer className="bg-black text-light pt-4 pb-3 border-top border-secondary mt-auto text-center">{"\n"}<div className="container"><p className="mb-0 text-white-50 small">{"© 2026 Ñackson Store. Todos los derechos reservados."}</p></div>{"\n"}</footer>
  );
}

function HomeFooter() {
  return (
<footer className="bg-black text-light pt-5 pb-3 border-top border-secondary mt-auto">{"\n"}<div className="container">{"\n"}<div className="row g-4 mb-4">{"\n"}<div className="col-md-4">{"\n"}<h5 className="text-uppercase text-primary fw-bold mb-3">{"Ñackson Store"}</h5>{"\n"}<p className="text-white-50 small mb-3">{"Revive la nostalgia. Siente el poder de lo físico. Tu tienda especializada en entretenimiento físico y artículos de colección."}</p>{"\n"}<div className="d-flex gap-3 fs-5 text-light">{"\n"}<a href="#" className="text-light hover-light"><i className="bi bi-instagram"></i></a>{"\n"}<a href="#" className="text-light hover-light"><i className="bi bi-facebook"></i></a>{"\n"}<a href="#" className="text-light hover-light"><i className="bi bi-tiktok"></i></a>{"\n"}<a href="#" className="text-light hover-light"><i className="bi bi-youtube"></i></a>{"\n"}</div>{"\n"}</div>{"\n"}<div className="col-md-4">{"\n"}<h5 className="text-uppercase text-light fw-bold fs-6 mb-3">{"Información & Ayuda"}</h5>{"\n"}<ul className="list-unstyled small text-white-50">{"\n"}<li className="mb-2"><a href="#" className="text-decoration-none text-white-50 hover-light">{"Quienes somos"}</a></li>{"\n"}<li className="mb-2"><a href="#" className="text-decoration-none text-white-50 hover-light">{"Encuéntranos aquí"}</a></li>{"\n"}<li className="mb-2"><a href="#" className="text-decoration-none text-white-50 hover-light">{"Despachos y Envíos"}</a></li>{"\n"}<li className="mb-2"><a href="#" className="text-decoration-none text-white-50 hover-light">{"Términos y condiciones"}</a></li>{"\n"}<li className="mb-2"><a href="#" className="text-decoration-none text-white-50 hover-light">{"Políticas de cambios y devoluciones"}</a></li>{"\n"}</ul>{"\n"}</div>{"\n"}<div className="col-md-4">{"\n"}<h5 className="text-uppercase text-light fw-bold fs-6 mb-3">{"Contacto Ñackson"}</h5>{"\n"}<p className="mb-2 text-white-50 small"><i className="bi bi-envelope-fill me-2 text-primary"></i>{" contacto@nacksonstore.cl"}</p>{"\n"}<p className="mb-2 text-white-50 small"><i className="bi bi-geo-alt-fill me-2 text-primary"></i>{" Santiago, Chile"}</p>{"\n"}<div className="mt-4 p-3 bg-dark rounded border border-secondary">{"\n"}<p className="small text-white-50 mb-2 fw-bold">{"Paga seguro con tu banco:"}</p>{"\n"}<div className="d-flex align-items-center justify-content-between gap-2 flex-wrap">{"\n"}<span className="badge bg-danger text-white">{"Redcompra"}</span>{"\n"}<span className="badge bg-primary text-white">{"Visa"}</span>{"\n"}<span className="badge bg-warning text-dark">{"Mastercard"}</span>{"\n"}<span className="badge bg-secondary text-white">{"Banco Estado"}</span>{"\n"}<span className="small text-white-50">{"+ y más"}</span>{"\n"}</div>{"\n"}<div className="mt-2 text-end">{"\n"}<span className="text-info fw-bold small" style={{"fontFamily": "monospace"}}>{"webpay"}<span className="text-light">{".PLUS"}</span></span>{"\n"}</div>{"\n"}</div>{"\n"}</div>{"\n"}</div>{"\n"}<hr className="border-secondary" />{"\n"}<div className="row align-items-center">{"\n"}<div className="col-md-6 text-center text-md-start">{"\n"}<p className="mb-0 text-white-50 small">{"© 2026 Ñackson Store. Creado por Seba. Todos los derechos reservados."}</p>{"\n"}</div>{"\n"}<div className="col-md-6 text-center text-md-end mt-2 mt-md-0">{"\n"}<span className="text-white-50 small">{"Desarrollado con Bootstrap 5 y tecnologías web modernas"}</span>{"\n"}</div>{"\n"}</div>{"\n"}</div>{"\n"}</footer>
  );
}

function AccessoriesFooter() {
  return (
<footer className="bg-black text-light pt-5 pb-3 border-top border-secondary mt-auto">{"\n"}<div className="container text-center">{"\n"}<p className="mb-0 text-white-50 small">{"© 2026 Ñackson Store. Revive la nostalgia. Siente el poder de lo físico. Todos los derechos reservados."}</p>{"\n"}</div>{"\n"}</footer>
  );
}

function AdminFooter() {
  return (
<footer className="bg-black text-light pt-5 pb-3 border-top border-secondary mt-auto">{"\n"}<div className="container text-center">{"\n"}<p className="mb-0 text-white-50 small">{"© 2026 Ñackson Store. Creado por Seba. Todos los derechos reservados."}</p>{"\n"}</div>{"\n"}</footer>
  );
}

function CartFooter() {
  return (
<footer className="bg-black text-light text-center py-3 mt-auto border-top border-secondary">{"\n"}<div className="container">{"\n"}<p className="mb-0 text-white-50 small">{"© 2026 Ñackson Store. Todos los derechos reservados."}</p>{"\n"}</div>{"\n"}</footer>
  );
}

function CheckoutFooter() {
  return (
<footer className="bg-black text-center py-3 border-top border-secondary mt-auto text-white-50 small">{"\n        © 2026 Ñackson Store - Integración TypeScript & Spring Boot\n    "}</footer>
  );
}

function ResultsFooter() {
  return (
<footer className="bg-black text-light pt-5 pb-3 border-top border-secondary mt-auto">{"\n"}<div className="container text-center">{"\n"}<p className="mb-0 text-white-50 small">{"© 2026 Ñackson Store. Creado por Seba."}</p>{"\n"}</div>{"\n"}</footer>
  );
}
