import { createContext, useContext, useEffect, useState, type ReactNode } from 'react';
import { createPortal } from 'react-dom';

type Theme = 'dark' | 'light';
const STORAGE_KEY = 'nackson-store-tema';
// La paleta clásica es la acordada por el equipo. Los otros temas tienen identidad propia.
const PALETTES = {
  clasico: { nombre: "Clásico 2000's", colores: ['#053F5C','#429EBD','#9FE7F5','#F7AD19','#F27F0C'] },
  gta: { nombre: 'GTA VI', colores: ['#211533','#684481','#F79DC4','#FFC17E','#EC528C'] },
  minecraft: { nombre: 'Minecraft', colores: ['#18291E','#387843','#A1D77A','#D2B16F','#855337'] },
  stranger: { nombre: 'Stranger Things', colores: ['#180B14','#65192B','#FF384C','#F8C0B8','#AB263D'] },
  anime: { nombre: 'Anime', colores: ['#292044','#615391','#E1D6FF','#FFC857','#F28C28'] },
  cine: { nombre: 'Cine', colores: ['#161B26','#39465C','#B7C9EB','#D5B77B','#E5DCE8'] },
  neon: { nombre: 'Modo Neón', colores: ['#10112D','#3434A0','#00F5FF','#C64BFF','#FF39AA'] },
} as const;
type Palette = keyof typeof PALETTES;
type Preferences = { theme: Theme; palette: Palette; opacity: number; glow: number; custom: string[] | null };
const DEFAULT: Preferences = { theme: 'dark', palette: 'clasico', opacity: 75, glow: 60, custom: null };
const HEX = /^#[0-9a-f]{6}$/i;
const ThemeContext = createContext<{ prefs: Preferences; update: (patch: Partial<Preferences>) => void; reset: () => void } | null>(null);
function savedPreferences(): Preferences {
  try {
    const raw = localStorage.getItem(`${STORAGE_KEY}-v3`);
    if (!raw) return { ...DEFAULT, theme: localStorage.getItem(STORAGE_KEY) === 'light' ? 'light' : 'dark' };
    const p = JSON.parse(raw) as Partial<Preferences>;
    return { theme: p.theme === 'light' ? 'light' : 'dark',
      palette: p.palette && Object.prototype.hasOwnProperty.call(PALETTES, p.palette) ? p.palette : 'clasico',
      opacity: typeof p.opacity === 'number' && Number.isFinite(p.opacity) ? Math.max(35, Math.min(100, p.opacity)) : 65,
      glow: typeof p.glow === "number" && Number.isFinite(p.glow) ? Math.max(0, Math.min(100,p.glow)) : 60,
      custom: Array.isArray(p.custom) && p.custom.length === 5 && p.custom.every(c => typeof c === 'string' && HEX.test(c)) ? p.custom : null };
  } catch { return DEFAULT; }
}
function colorOn(hex: string) {
  const rgb = [1,3,5].map(i => parseInt(hex.slice(i,i+2),16)/255).map(v => v <= .04045 ? v/12.92 : ((v+.055)/1.055)**2.4);
  return .2126*rgb[0]+.7152*rgb[1]+.0722*rgb[2] > .179 ? '#111827' : '#FFFFFF';
}
export function NacksonThemeProvider({ children }: { children: ReactNode }) {
  const [prefs, setPrefs] = useState<Preferences>(savedPreferences);
  useEffect(() => {
    const root = document.documentElement;
    const colors = prefs.custom ?? PALETTES[prefs.palette].colores;
    root.setAttribute('data-nackson-theme', prefs.theme);
    root.setAttribute('data-nackson-palette', prefs.custom ? 'custom' : prefs.palette);
    root.style.colorScheme = prefs.theme;
    ['base','medium','accent','gold','orange'].forEach((key,i) => root.style.setProperty(`--nx-${key}`,colors[i]));
    root.style.setProperty('--nx-on-gold',colorOn(colors[3]));
    root.style.setProperty('--nx-on-accent',colorOn(colors[2]));
    root.style.setProperty('--nx-opacity',`${prefs.opacity}%`);
    root.style.setProperty('--nx-glow',`${prefs.glow}%`);
    try { localStorage.setItem(STORAGE_KEY,prefs.theme); localStorage.setItem(`${STORAGE_KEY}-v3`,JSON.stringify(prefs)); } catch { /* almacenamiento opcional */ }
  }, [prefs]);
  return <ThemeContext.Provider value={{ prefs, update: patch => setPrefs(p => ({ ...p, ...patch })), reset: () => setPrefs(DEFAULT) }}>{children}</ThemeContext.Provider>;
}

/** Conserva el botón en todas las variantes de Navbar; añade el panel de personalización. */
export function NacksonThemeNavbarButton() {
  const ctx = useContext(ThemeContext);
  const [target, setTarget] = useState<Element | null>(null);
  const [open, setOpen] = useState(false);
  useEffect(() => {
    const find = () => setTarget(document.querySelector('.nackson-legacy-page .navbar .container'));
    find();
    const observer = new MutationObserver(find);
    observer.observe(document.getElementById('root') ?? document.body,{childList:true,subtree:true});
    return () => observer.disconnect();
  }, []);
  useEffect(() => {
    if (!open) return;
    const close = (e: KeyboardEvent) => { if (e.key === 'Escape') setOpen(false); };
    window.addEventListener('keydown',close); return () => window.removeEventListener('keydown',close);
  },[open]);
  if (!ctx) return null;
  const {prefs,update,reset} = ctx;
  const colors = prefs.custom ?? PALETTES[prefs.palette].colores;
  const button = <div className="nx-theme-control">
    <button type="button" className="nackson-theme-toggle" onClick={() => setOpen(v=>!v)} aria-expanded={open} aria-label="Personalizar apariencia" title="Personalizar apariencia"><i className="bi bi-palette" aria-hidden="true"/><span className="nackson-theme-label">Temas</span></button>
    {open && createPortal(<aside className="nx-theme-panel" aria-label="Personaliza tu experiencia">
      <header><strong>Personaliza tu experiencia</strong><button type="button" onClick={()=>setOpen(false)} aria-label="Cerrar personalización">×</button></header>
      <p className="nx-theme-kicker">ÑACKSON STORE / EST. 2000'S</p>
      <button type="button" className="nx-theme-mode" onClick={()=>update({theme:prefs.theme==='dark'?'light':'dark'})}>{prefs.theme==='dark'?'☀ Modo claro':'☾ Modo oscuro'}</button>
      <div className="nx-theme-list">{(Object.keys(PALETTES) as Palette[]).map(key=><button type="button" key={key} aria-pressed={!prefs.custom&&prefs.palette===key} onClick={()=>update({palette:key,custom:null})}><span>{PALETTES[key].nombre}</span><span className="nx-swatches">{PALETTES[key].colores.map(c=><i key={c} style={{backgroundColor:c}}/>)}</span></button>)}</div>
      <label className="nx-opacity">Opacidad de los paneles <strong>{prefs.opacity}%</strong><input type="range" min="35" max="100" value={prefs.opacity} onChange={e=>update({opacity:Number(e.target.value)})}/></label>
      <div className="nx-custom-colors">{colors.map((color,i)=><label key={i}>{['Fondo','Paneles','Acento','Botones','Detalle'][i]}<input type="color" value={color} onChange={e=>update({custom:[...colors].map((c,j)=>j===i?e.target.value:c)})}/></label>)}</div>
      <button type="button" className="nx-theme-mode" onClick={reset}>Restablecer tema original</button>
    </aside>,document.body)}
  </div>;
  return target ? createPortal(button,target) : <div className="nackson-theme-fallback">{button}</div>;
}

export function HomeThemePanel({onClose}:{onClose:()=>void}) {
 const ctx=useContext(ThemeContext); const [customOpen,setCustomOpen]=useState(false);
 if(!ctx) return null;
 const {prefs,update,reset}=ctx; const colors=prefs.custom??PALETTES[prefs.palette].colores;
 const order:Palette[]=['clasico','stranger','minecraft','gta','anime','cine','neon'];
 const icons=['tv','lightning-charge','box','controller','stars','film','buildings'];
 return <aside className="nx-home-themes nx-glass" aria-label="Temas"><header><div><h2>✦ Temas</h2><small>Personaliza tu experiencia</small></div><button aria-label="Cerrar temas" onClick={onClose}>×</button></header>
 <div className="nx-home-theme-list">{order.map((key,i)=><button key={key} aria-pressed={!prefs.custom&&prefs.palette===key} onClick={()=>update({palette:key,custom:null})}><span className="nx-theme-art" style={{background:`linear-gradient(135deg,${PALETTES[key].colores[0]},${PALETTES[key].colores[1]},${PALETTES[key].colores[2]})`}}><i className={`bi bi-${icons[i]}`}/></span><span>{PALETTES[key].nombre}<span className="nx-swatches">{PALETTES[key].colores.map(c=><i key={c} style={{background:c}}/>)}</span></span><i className="bi bi-circle"/></button>)}</div>
 <button className="nx-home-theme-action" onClick={()=>setCustomOpen(v=>!v)}><i className="bi bi-palette"/> Personalizar colores</button>
 {customOpen&&<div className="nx-custom-colors">{colors.map((c,i)=><label key={i}>{['Fondo','Panel','Acento','Botón','Detalle'][i]}<input type="color" value={c} onChange={e=>update({custom:[...colors].map((v,j)=>i===j?e.target.value:v)})}/></label>)}</div>}
 <label className="nx-home-slider">Opacidad <strong>{prefs.opacity}%</strong><input type="range" min="35" max="100" value={prefs.opacity} onChange={e=>update({opacity:Number(e.target.value)})}/></label>
 <label className="nx-home-slider">Brillo del neón <strong>{prefs.glow}%</strong><input type="range" min="0" max="100" value={prefs.glow} onChange={e=>update({glow:Number(e.target.value)})}/></label>
 <div className="nx-theme-bottom"><button onClick={()=>update({theme:prefs.theme==='dark'?'light':'dark'})}>{prefs.theme==='dark'?'☀ Claro':'☾ Oscuro'}</button><button onClick={reset}>Restablecer</button></div></aside>;
}
