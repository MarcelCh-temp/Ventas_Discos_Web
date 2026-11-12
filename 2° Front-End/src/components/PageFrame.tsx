import { useEffect, useState, type FormEvent, type MouseEvent, type ReactNode } from 'react';
import { useNavigate } from 'react-router-dom';

type Props = {
  title: string;
  css: string;
  bodyClass?: string;
  bodyStyle?: Record<string, string>;
  children: ReactNode;
};

/**
 * Encapsula CSS y clases de la página original. Su objetivo es mantener su apariencia.
 * Bloquea operaciones del JavaScript antiguo que simulaban autorización, pedidos o sesiones.
 */
export function PageFrame({ title, css, bodyClass = '', bodyStyle, children }: Props) {
  const [notice, setNotice] = useState('');
  const navigate = useNavigate();
  useEffect(() => {
    document.title = title;
    const oldClasses = document.body.className;
    const oldStyle = document.body.getAttribute('style');
    document.body.className = bodyClass;
    if (bodyStyle && Object.keys(bodyStyle).length > 0) {
      Object.assign(document.body.style, bodyStyle);
    }
    const styleElement = document.createElement('style');
    styleElement.setAttribute('data-nackson-page', title);
    styleElement.textContent = css;
    document.head.appendChild(styleElement);
    return () => {
      styleElement.remove();
      document.body.className = oldClasses;
      if (oldStyle === null) document.body.removeAttribute('style');
      else document.body.setAttribute('style', oldStyle);
    };
  }, [title, css, bodyClass, bodyStyle]);

  function handleSubmit(event: FormEvent<HTMLDivElement>) {
    const form = event.target;
    if (!(form instanceof HTMLFormElement)) return;
    // Los formularios React modernos gestionan su submit y sus errores directamente.
    // No interceptarlos: esta protección solo corresponde a formularios HTML heredados.
    if (form.hasAttribute('data-nackson-react-form')) return;
    event.preventDefault();
    const action = form.getAttribute('action') || '';
    if (action.startsWith('resultados.html')) {
      const data = new FormData(form);
      const query = String(data.get('q') || '').trim();
      navigate('/resultados.html?q=' + encodeURIComponent(query));
      return;
    }
    setNotice('Esta acción todavía no está conectada al backend. No se ha creado ninguna sesión, compra ni registro.');
  }

  function handleClick(event: MouseEvent<HTMLDivElement>) {
    const target = event.target;
    if (!(target instanceof Element)) return;
    const originalButton = target.closest('[data-legacy-onclick]');
    if (!originalButton) return;
    event.preventDefault();
    event.stopPropagation();
    setNotice('Esta interacción se migrará a TypeScript y React en una próxima etapa. No se ejecutará la función antigua.');
  }

  return (
    <div onSubmitCapture={handleSubmit} onClickCapture={handleClick} className="nackson-legacy-page">
      {children}
      {notice && (
        <div role="status" aria-live="polite" className="alert alert-warning position-fixed bottom-0 start-50 translate-middle-x shadow" style={{ zIndex: 3000, width: 'min(90vw, 630px)' }}>
          <div className="d-flex align-items-center justify-content-between gap-3">
            <span>{notice}</span>
            <button type="button" className="btn-close" aria-label="Cerrar aviso" onClick={() => setNotice('')} />
          </div>
        </div>
      )}
    </div>
  );
}
