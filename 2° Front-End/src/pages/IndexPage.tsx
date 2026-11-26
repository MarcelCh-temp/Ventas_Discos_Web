import {useEffect,useState} from 'react';
import {Link} from 'react-router-dom';
import {useAuth} from '../state/AuthContext';
import {NacksonShell,categories} from '../components/NacksonShell';
import {RealCatalog} from '../components/RealCatalog';
import type {Categoria} from '../services/storeApi';
const filters: {name:string;icon:string;cat?:Categoria;genre?:string}[]=[{name:'Todo',icon:'star-fill'},{name:'Películas',icon:'film',cat:'SERIES_PELICULAS_ANIME',genre:'Película'},{name:'Series',icon:'tv',cat:'SERIES_PELICULAS_ANIME',genre:'Serie'},{name:'Música',icon:'music-note-beamed',cat:'MUSICA'},{name:'Videojuegos',icon:'controller',cat:'VIDEOJUEGOS'},{name:'Merch',icon:'bag',cat:'MERCHANDISING'}];
const slides=[{title:'COLECCIONA',accent:'TU ENTRETENIMIENTO',text:'Música, películas, series, anime, videojuegos y merch en un solo lugar.',img:'/img/nx-hero-retro.jpg',to:'#nx-destacados',cta:'Explorar ahora'},{title:'REVIVE',accent:'LA NOSTALGIA',text:'Encuentra tus películas y series favoritas en formato físico.',img:'/img/cine.jpg',to:'/peliculas.html',cta:'Ver películas'},{title:'SUBE',accent:'EL VOLUMEN',text:'Descubre música y coleccionables para tu colección.',img:'/img/rock.jpg',to:'/musica.html',cta:'Explorar música'}];
export default function IndexPage(){
 const {session}=useAuth(); const [slide,setSlide]=useState(0);const [filter,setFilter]=useState(0);
 useEffect(()=>{document.title='Ñackson Store';},[]);
 const hero=slides[slide]; const selected=filters[filter];
 return <NacksonShell home> <main className="nx-home-main"><section className="nx-hero nx-glass" aria-label="Promociones"><div className="nx-hero-art" style={{backgroundImage:`url(${hero.img})`}}/><div className="nx-hero-copy"><small>✦ ÑACKSON STORE</small><h1>{hero.title}<br/><em>{hero.accent}</em></h1><p>{hero.text}</p>{hero.to.startsWith('#')?<a className="nx-cta" href={hero.to}>{hero.cta} →</a>:<Link className="nx-cta" to={hero.to}>{hero.cta} →</Link>}</div><button className="nx-hero-prev" aria-label="Promoción anterior" onClick={()=>setSlide((slide+slides.length-1)%slides.length)}>‹</button><button className="nx-hero-next" aria-label="Siguiente promoción" onClick={()=>setSlide((slide+1)%slides.length)}>›</button><div className="nx-hero-dots">{slides.map((_,i)=><button key={i} aria-label={`Promoción ${i+1}`} aria-pressed={slide===i} onClick={()=>setSlide(i)}/>)}</div></section>
 <section className="nx-entertain nx-glass"><div className="nx-entertain-side"><h2><i className="bi bi-film"/> Entretenimiento</h2><div className="nx-filter-tiles">{filters.map((f,i)=><button key={f.name} aria-pressed={filter===i} onClick={()=>setFilter(i)}><i className={`bi bi-${f.icon}`}/>{f.name}</button>)}</div></div><div className="nx-category-strip">{categories.slice(1,9).map(([name,to,,img])=><Link to={to} key={to}><img src={`/img/cat_${img}.jpg`} alt=""/><span>{name}</span><small>Explorar →</small></Link>)}</div></section>
 <section className="nx-featured" id="nx-destacados"><header><h2>✦ Novedades y destacados</h2><Link to="/resultados.html">Ver todo →</Link></header><RealCatalog key={filter} categoria={selected.cat} genero={selected.genre} compact limit={6}/></section><footer className="nx-home-footer">Ñackson Store · Entretenimiento físico <Link to="/ofertas">Ofertas</Link>{session?.rol==='CLIENTE'&&<Link to="/soporte">Ayuda</Link>}</footer></main>
</NacksonShell>;
}
