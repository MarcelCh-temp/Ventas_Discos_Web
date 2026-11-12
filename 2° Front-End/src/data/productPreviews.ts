/** Productos de ejemplo del HTML anterior; no proceden de Catalog, no representan stock real. */
import type { CategoryProductPreview, FeaturedProductPreview } from '../models/ProductPreview';
export const categoryProductPreviews = {
  musica: [{"title": "Álbum Vinilo Edición Coleccionista", "description": "Vinilo de alta fidelidad con arte exclusivo impreso.", "priceText": "$27.990", "iconClassName": "bi bi-vinyl text-danger display-4", "legacyAction": "comprarMusicaDirecto()"}],
  series: [{"title": "Serie de Televisión - Box Set Completo", "description": "Todas las temporadas en formato físico.", "priceText": "$34.990", "iconClassName": "bi bi-camera-reels text-danger display-4", "legacyAction": "comprarSerieDirecto()"}],
  peliculas: [{"title": "Película Clásica de Culto (Edición Limitada)", "description": "Remasterizada en alta definición con contenido extra.", "priceText": "$19.990", "iconClassName": "bi bi-camera-video text-danger display-4", "legacyAction": "comprarPeliculaDirecto()"}],
  anime: [{"title": "Box Set Anime Edición Coleccionista", "description": "Serie completa remasterizada en alta definición con extras.", "priceText": "$32.990", "iconClassName": "bi bi-play-btn-fill text-danger display-4", "legacyAction": "comprarAnimeDirecto()"}],
  bluray: [{"title": "Disco Blu-ray Estándar", "description": "Alta definición con sonido envolvente.", "priceText": "$14.990", "iconClassName": "bi bi-disc-fill text-danger display-4", "legacyAction": "comprarBlurayDirecto()"}],
  videojuegos: [{"title": "Juego Retro Edición Coleccionista", "description": "Versión física completa con manual impreso y caja especial.", "priceText": "$39.990", "iconClassName": "bi bi-joystick text-danger display-4", "legacyAction": "comprarJuegoDirecto()"}],
  merchandising: [{"title": "Figura Coleccionable Edición Especial", "description": "Figura pintada a mano con base de exhibición.", "priceText": "$45.990", "iconClassName": "bi bi-gift text-danger display-4", "legacyAction": "comprarMerchDirecto()"}],
  accesorios: [{"title": "Llavero Metálico Coleccionable", "description": "Diseño exclusivo de edición limitada.", "priceText": "$8.990", "iconClassName": "bi bi-gem text-danger display-4", "legacyAction": "comprarAccesorioDirecto()"}],
} satisfies Record<string, readonly CategoryProductPreview[]>;

export const featuredProductPreviews: readonly FeaturedProductPreview[] = [
  {
    "title": "Neon Genesis Evangelion - Box Set",
    "description": "Edición coleccionista en formato Blu-ray.",
    "priceText": "$29.990",
    "imageSrc": "/img/evangelion.jpg",
    "imageAlt": "Neon Genesis Evangelion",
    "linkTo": "/anime.html",
    "linkLabel": "Ver en Anime"
  },
  {
    "title": "Álbum Rock Clásico (Vinilo)",
    "description": "Edición remasterizada de alta fidelidad.",
    "priceText": "$24.990",
    "imageSrc": "https://images.unsplash.com/photo-1539185441755-769473a23570?q=80&w=600&auto=format&fit=crop",
    "imageAlt": "Álbum Rock Clásico",
    "linkTo": "/musica.html",
    "linkLabel": "Ver en Música"
  },
  {
    "title": "Cyberpunk Edgerunners - Blu-ray",
    "description": "Serie completa con extras exclusivos.",
    "priceText": "$21.990",
    "imageSrc": "/img/cyberpunk.jpg",
    "imageAlt": "Cyberpunk Edgerunners",
    "linkTo": "/series.html",
    "linkLabel": "Ver en Series"
  }
];
