import { useEffect, useState, type FormEvent } from 'react';
import { useSearchParams } from 'react-router-dom';
import { StoreLayout } from '../components/StoreLayout';
import { RealCatalog } from '../components/RealCatalog';

export default function RealResultadosPage() {
  const [params, setParams] = useSearchParams();
  const q = params.get('q')?.trim() || '';
  const [busqueda, setBusqueda] = useState(q);

  // Sincronizar el texto con el enlace y con Atrás/Adelante del navegador.
  useEffect(() => { setBusqueda(q); }, [q]);

  const buscar = (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    const siguientes = new URLSearchParams(params);
    const texto = busqueda.trim();
    if (texto) siguientes.set('q', texto);
    else siguientes.delete('q');
    setParams(siguientes);
  };

  const limpiar = () => {
    setBusqueda('');
    const siguientes = new URLSearchParams(params);
    siguientes.delete('q');
    setParams(siguientes);
  };

  return (
    <StoreLayout title="Buscar productos">
      <form role="search" data-nackson-react-form className="mb-4" onSubmit={buscar}>
        <label htmlFor="busqueda-producto" className="form-label text-light">
          Buscar por nombre de producto
        </label>
        <div className="input-group">
          <input
            id="busqueda-producto"
            type="search"
            className="form-control bg-dark text-light border-secondary"
            placeholder="Ej.: Alejandro, Naruto, Beatles..."
            value={busqueda}
            onChange={e => setBusqueda(e.target.value)}
          />
          <button type="submit" className="btn btn-outline-info">Buscar</button>
          {q && <button type="button" className="btn btn-outline-light" onClick={limpiar}>Limpiar</button>}
        </div>
      </form>
      <RealCatalog key={q} q={q} titulo={q ? `Resultados para «${q}»` : 'Catálogo completo'} />
    </StoreLayout>
  );
}
