import { createContext, useContext, useEffect, useState, type ReactNode } from 'react';
import { createPortal } from 'react-dom';

type Theme = 'dark' | 'light';
const STORAGE_KEY = 'nackson-store-tema';
const ThemeContext = createContext<{ theme: Theme; toggle: () => void } | null>(null);

function savedTheme(): Theme {
  try { return window.localStorage.getItem(STORAGE_KEY) === 'light' ? 'light' : 'dark'; }
  catch { return 'dark'; }
}

export function NacksonThemeProvider({ children }: { children: ReactNode }) {
  const [theme, setTheme] = useState<Theme>(savedTheme);
  useEffect(() => {
    document.documentElement.setAttribute('data-nackson-theme', theme);
    document.documentElement.style.colorScheme = theme;
    try { window.localStorage.setItem(STORAGE_KEY, theme); } catch { /* almacenamiento opcional */ }
  }, [theme]);
  return <ThemeContext.Provider value={{ theme, toggle: () => setTheme(v => v === 'dark' ? 'light' : 'dark') }}>
    {children}
  </ThemeContext.Provider>;
}

/** Botón único, montado dentro de la barra vigente; no altera las 9 variantes ni sus enlaces. */
export function NacksonThemeNavbarButton() {
  const ctx = useContext(ThemeContext);
  const [target, setTarget] = useState<Element | null>(null);
  useEffect(() => {
    // Navbar se monta antes que este componente. Si la ruta cambia se detecta de nuevo.
    const find = () => setTarget(document.querySelector('.navbar-futurista .container'));
    find();
    const observer = new MutationObserver(find);
    observer.observe(document.getElementById('root') ?? document.body, { childList: true, subtree: true });
    return () => observer.disconnect();
  }, []);
  if (!ctx) return null;
  const button = <button
    type="button"
    className="nackson-theme-toggle"
    onClick={ctx.toggle}
    aria-label={ctx.theme === 'dark' ? 'Activar modo claro' : 'Activar modo oscuro'}
    title={ctx.theme === 'dark' ? 'Cambiar a modo claro' : 'Cambiar a modo oscuro'}
    aria-pressed={ctx.theme === 'light'}
  >
    <span aria-hidden="true">{ctx.theme === 'dark' ? '☀' : '☾'}</span>
    <span className="nackson-theme-label">{ctx.theme === 'dark' ? 'Modo día' : 'Modo noche'}</span>
  </button>;
  return target ? createPortal(button, target) : <div className="nackson-theme-fallback">{button}</div>;
}
