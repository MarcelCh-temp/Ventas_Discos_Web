
/** Footers de las vistas originales con variantes tipadas. */

export type FooterVariant =
  | 'category'
  | 'home'
  | 'accessories'
  | 'admin'
  | 'cart'
  | 'checkout'
  | 'results';

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

const copyright = '© 2026 Ñackson Store.';

function CategoryFooter() {
  return (
    <footer className="bg-black text-light pt-4 pb-3 border-top border-secondary mt-auto text-center">
      <div className="container">
        <p className="mb-0 text-white-50 small">{copyright}</p>
      </div>
    </footer>
  );
}

function HomeFooter() {
  return (
    <footer className="bg-black text-light pt-5 pb-3 border-top border-secondary mt-auto">
      <div className="container">
        <div className="row g-4 mb-4">
          <div className="col-md-4">
            <h5 className="text-uppercase text-primary fw-bold mb-3">
              Ñackson Store
            </h5>

            <p className="text-white-50 small mb-3">
              Revive la nostalgia. Siente el poder de lo físico.
              Tu tienda especializada en entretenimiento físico
              y artículos de colección.
            </p>

            <div className="d-flex gap-3 fs-5 text-light">
              <a href="#" className="text-light hover-light">
                <i className="bi bi-instagram"></i>
              </a>
              <a href="#" className="text-light hover-light">
                <i className="bi bi-facebook"></i>
              </a>
              <a href="#" className="text-light hover-light">
                <i className="bi bi-tiktok"></i>
              </a>
              <a href="#" className="text-light hover-light">
                <i className="bi bi-youtube"></i>
              </a>
            </div>
          </div>

          <div className="col-md-4">
            <h5 className="text-uppercase text-light fw-bold fs-6 mb-3">
              Información & Ayuda
            </h5>

            <ul className="list-unstyled small text-white-50">
              <li className="mb-2">
                <a href="#" className="text-decoration-none text-white-50 hover-light">
                  Quienes somos
                </a>
              </li>
              <li className="mb-2">
                <a href="#" className="text-decoration-none text-white-50 hover-light">
                  Encuéntranos aquí
                </a>
              </li>
              <li className="mb-2">
                <a href="#" className="text-decoration-none text-white-50 hover-light">
                  Despachos y Envíos
                </a>
              </li>
              <li className="mb-2">
                <a href="#" className="text-decoration-none text-white-50 hover-light">
                  Términos y condiciones
                </a>
              </li>
              <li className="mb-2">
                <a href="#" className="text-decoration-none text-white-50 hover-light">
                  Políticas de cambios y devoluciones
                </a>
              </li>
            </ul>
          </div>

          <div className="col-md-4">
            <h5 className="text-uppercase text-light fw-bold fs-6 mb-3">
              Contacto Ñackson
            </h5>

            <p className="mb-2 text-white-50 small">
              <i className="bi bi-envelope-fill me-2 text-primary"></i>
              {' contacto@nacksonstore.cl'}
            </p>

            <p className="mb-2 text-white-50 small">
              <i className="bi bi-geo-alt-fill me-2 text-primary"></i>
              {' Santiago, Chile'}
            </p>

            <div className="mt-4 p-3 bg-dark rounded border border-secondary">
              <p className="small text-white-50 mb-2 fw-bold">
                Paga seguro con tu banco:
              </p>

              <div className="d-flex align-items-center justify-content-between gap-2 flex-wrap">
                <span className="badge bg-danger text-white">Redcompra</span>
                <span className="badge bg-primary text-white">Visa</span>
                <span className="badge bg-warning text-dark">Mastercard</span>
                <span className="badge bg-secondary text-white">Banco Estado</span>
                <span className="small text-white-50">+ y más</span>
              </div>

              <div className="mt-2 text-end">
                <span
                  className="text-info fw-bold small"
                  style={{ fontFamily: 'monospace' }}
                >
                  webpay
                  <span className="text-light">.PLUS</span>
                </span>
              </div>
            </div>
          </div>
        </div>

        <hr className="border-secondary" />

        <div className="text-center">
          <p className="mb-0 text-white-50 small">{copyright}</p>
        </div>
      </div>
    </footer>
  );
}

function AccessoriesFooter() {
  return (
    <footer className="bg-black text-light pt-5 pb-3 border-top border-secondary mt-auto">
      <div className="container text-center">
        <p className="mb-0 text-white-50 small">{copyright}</p>
      </div>
    </footer>
  );
}

function AdminFooter() {
  return (
    <footer className="bg-black text-light pt-5 pb-3 border-top border-secondary mt-auto">
      <div className="container text-center">
        <p className="mb-0 text-white-50 small">{copyright}</p>
      </div>
    </footer>
  );
}

function CartFooter() {
  return (
    <footer className="bg-black text-light text-center py-3 mt-auto border-top border-secondary">
      <div className="container">
        <p className="mb-0 text-white-50 small">{copyright}</p>
      </div>
    </footer>
  );
}

function CheckoutFooter() {
  return (
    <footer className="bg-black text-center py-3 border-top border-secondary mt-auto text-white-50 small">
      {copyright}
    </footer>
  );
}

function ResultsFooter() {
  return (
    <footer className="bg-black text-light pt-5 pb-3 border-top border-secondary mt-auto">
      <div className="container text-center">
        <p className="mb-0 text-white-50 small">{copyright}</p>
      </div>
    </footer>
  );
}
