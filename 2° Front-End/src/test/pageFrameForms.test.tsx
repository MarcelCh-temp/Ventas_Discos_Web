import type { FormEvent, ReactNode } from 'react';
import { describe, expect, it, vi } from 'vitest';
import { fireEvent, render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { PageFrame } from '../components/PageFrame';

function view(children: ReactNode) {
  return render(
    <MemoryRouter>
      <PageFrame title="Prueba formularios" css="">
        {children}
      </PageFrame>
    </MemoryRouter>,
  );
}

describe('Protección de formularios antiguos sin interferir con Accounts', () => {
  it('deja que un formulario React gestione su propio submit sin aviso heredado', () => {
    const onSubmit = vi.fn((event: FormEvent<HTMLFormElement>) => event.preventDefault());
    view(<form data-nackson-react-form="true" onSubmit={onSubmit}><button type="submit">Entrar</button></form>);
    fireEvent.submit(screen.getByRole('button', { name: 'Entrar' }).closest('form')!);
    expect(onSubmit).toHaveBeenCalledOnce();
    expect(screen.queryByText(/esta acción todavía no está conectada al backend/i)).not.toBeInTheDocument();
  });

  it('sigue bloqueando formularios HTML heredados sin handler propio', () => {
    view(<form><button type="submit">Enviar formulario antiguo</button></form>);
    fireEvent.submit(screen.getByRole('button', { name: 'Enviar formulario antiguo' }).closest('form')!);
    expect(screen.getByText(/esta acción todavía no está conectada al backend/i)).toBeInTheDocument();
  });
});
