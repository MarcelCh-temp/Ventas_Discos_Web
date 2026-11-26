-- phpMyAdmin SQL Dump
-- version 5.2.3
-- https://www.phpmyadmin.net/
--
-- Servidor: 127.0.0.1
-- Tiempo de generación: 21-11-2026 a las 20:42:40
-- Versión del servidor: 10.11.16-MariaDB
-- Versión de PHP: 8.3.36

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
START TRANSACTION;
SET time_zone = "+00:00";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;

--
-- Base de datos: `db_catalog`
--

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `favorito`
--

CREATE TABLE `favorito` (
  `id` bigint(20) NOT NULL,
  `user_id` bigint(20) NOT NULL,
  `producto_id` bigint(20) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `inventario`
--

CREATE TABLE `inventario` (
  `id` bigint(20) NOT NULL,
  `product_id` bigint(20) NOT NULL,
  `stock_actual` int(11) NOT NULL,
  `umbral_critico` int(11) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Volcado de datos para la tabla `inventario`
--

INSERT INTO `inventario` (`id`, `product_id`, `stock_actual`, `umbral_critico`) VALUES
(1, 1, 10, 5),
(2, 2, 20, 5),
(3, 3, 0, 5),
(4, 4, 20, 5),
(5, 5, 20, 5),
(6, 6, 19, 5),
(7, 7, 15, 5),
(8, 8, 9, 5),
(9, 9, 15, 5),
(10, 10, 20, 5),
(11, 11, 15, 5),
(12, 12, 15, 5),
(13, 13, 10, 5),
(14, 14, 10, 5),
(15, 15, 10, 5),
(16, 16, 10, 5),
(17, 17, 12, 5),
(18, 18, 12, 5),
(19, 19, 12, 5),
(20, 20, 10, 5),
(21, 21, 20, 5),
(22, 22, 8, 5),
(23, 23, 10, 5),
(24, 24, 8, 5),
(25, 25, 12, 5),
(26, 26, 15, 5),
(38, 38, 12, 5),
(39, 39, 12, 5),
(40, 40, 8, 5),
(41, 41, 8, 5),
(42, 42, 12, 5),
(43, 43, 10, 5),
(44, 44, 10, 5),
(45, 45, 10, 5),
(46, 46, 10, 5),
(47, 47, 10, 5),
(48, 48, 10, 5),
(49, 49, 8, 5),
(50, 50, 12, 5),
(51, 51, 10, 5),
(52, 52, 12, 5),
(53, 53, 10, 5),
(54, 54, 10, 5),
(55, 55, 10, 5),
(56, 56, 10, 5),
(57, 57, 10, 5),
(58, 58, 10, 5),
(59, 59, 10, 5),
(60, 60, 10, 5),
(61, 61, 10, 5),
(62, 62, 10, 5),
(63, 63, 10, 5),
(64, 64, 10, 5),
(65, 65, 10, 5),
(66, 66, 10, 5),
(67, 67, 10, 5),
(68, 68, 10, 5),
(69, 69, 10, 5),
(70, 70, 10, 5),
(71, 71, 10, 5),
(72, 72, 10, 5),
(73, 73, 10, 5),
(74, 74, 10, 5),
(75, 75, 10, 5),
(76, 76, 10, 5),
(77, 77, 12, 5),
(78, 78, 10, 5),
(79, 79, 12, 5),
(80, 80, 12, 5),
(81, 81, 10, 5),
(82, 82, 10, 5),
(83, 83, 10, 5),
(84, 84, 10, 5),
(85, 85, 10, 5),
(86, 86, 10, 5),
(87, 87, 10, 5),
(88, 88, 10, 5),
(89, 89, 10, 5),
(90, 90, 10, 5),
(91, 91, 12, 5),
(92, 92, 10, 5),
(93, 93, 10, 5),
(94, 94, 10, 5),
(95, 95, 10, 5),
(96, 96, 10, 5),
(97, 97, 12, 5),
(98, 98, 10, 5),
(99, 99, 12, 5),
(100, 100, 10, 5),
(101, 101, 10, 5),
(102, 102, 10, 5),
(103, 103, 10, 5),
(104, 104, 10, 5),
(105, 105, 10, 5),
(106, 106, 10, 5),
(107, 107, 10, 5),
(108, 108, 10, 5),
(109, 109, 10, 5),
(110, 110, 12, 5),
(111, 111, 8, 5),
(112, 112, 10, 5),
(113, 113, 12, 5),
(114, 114, 10, 5),
(115, 115, 10, 5),
(116, 116, 10, 5),
(117, 117, 10, 5),
(118, 118, 10, 5),
(119, 119, 10, 5),
(120, 120, 10, 5),
(121, 121, 10, 5),
(122, 122, 12, 5),
(123, 123, 10, 5),
(124, 124, 10, 5),
(125, 125, 10, 5),
(126, 126, 10, 5),
(127, 127, 12, 5),
(128, 128, 10, 5),
(129, 129, 10, 5),
(130, 130, 10, 5),
(131, 131, 10, 5),
(132, 132, 10, 5),
(133, 133, 10, 5),
(134, 134, 10, 5),
(135, 135, 10, 5),
(136, 136, 10, 5),
(137, 137, 10, 5),
(138, 138, 10, 5),
(139, 139, 10, 5),
(140, 140, 10, 5),
(141, 141, 10, 5),
(142, 142, 10, 5),
(143, 143, 10, 5),
(144, 144, 10, 5),
(145, 145, 12, 5),
(146, 146, 10, 5),
(147, 147, 10, 5),
(148, 148, 10, 5),
(149, 149, 12, 5),
(150, 150, 12, 5),
(151, 151, 10, 5),
(152, 152, 10, 5),
(153, 153, 10, 5),
(154, 154, 12, 5),
(155, 155, 10, 5),
(165, 165, 0, 5),
(166, 166, 0, 5),
(167, 167, 0, 5),
(168, 168, 0, 5),
(169, 169, 0, 5),
(170, 170, 0, 5),
(171, 171, 0, 5);

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `movimiento_stock`
--

CREATE TABLE `movimiento_stock` (
  `id` bigint(20) NOT NULL,
  `cantidad` int(11) NOT NULL,
  `fecha_hora` datetime(6) NOT NULL,
  `motivo` varchar(255) DEFAULT NULL,
  `operacion_id` varchar(100) DEFAULT NULL,
  `stock_anterior` int(11) NOT NULL,
  `stock_resultante` int(11) NOT NULL,
  `tipo` enum('AJUSTE','ENTRADA','SALIDA') NOT NULL,
  `usuario_id` bigint(20) DEFAULT NULL,
  `inventario_id` bigint(20) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Volcado de datos para la tabla `movimiento_stock`
--

-- Historial de movimientos retirado del volcado.
-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `producto`
--

CREATE TABLE `producto` (
  `id` bigint(20) NOT NULL,
  `categoria` enum('ACCESORIOS','MERCHANDISING','MUSICA','SERIES_PELICULAS_ANIME','VIDEOJUEGOS') NOT NULL,
  `descripcion` varchar(2000) DEFAULT NULL,
  `estado` enum('ACTIVO','RETIRADO') NOT NULL,
  `formato` varchar(60) NOT NULL,
  `genero` varchar(60) NOT NULL,
  `imagen_url` varchar(500) DEFAULT NULL,
  `nombre` varchar(150) NOT NULL,
  `plataforma` varchar(60) DEFAULT NULL,
  `precio_base` decimal(12,2) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Volcado de datos para la tabla `producto`
--

INSERT INTO `producto` (`id`, `categoria`, `descripcion`, `estado`, `formato`, `genero`, `imagen_url`, `nombre`, `plataforma`, `precio_base`) VALUES
(1, 'MUSICA', 'Álbum de Lady Gaga en formato de 2 CD.', 'ACTIVO', '2CD', 'Pop', 'http://localhost:5173/productos/musica/lady-gaga.jpg', 'Lady Gaga — The Fame Monster (2 CD)', NULL, 14990.00),
(2, 'MUSICA', 'Álbum Innuendo de Queen en formato CD.', 'ACTIVO', '1CD', 'Rock', 'http://localhost:5173/productos/musica/queen.jpg', 'Queen — Innuendo (1 CD)', NULL, 25650.00),
(3, 'MUSICA', 'Producto creado para probar el inventario', 'RETIRADO', 'CD', 'ROCK', NULL, 'Producto Prueba Ñackson', NULL, 9.99),
(4, 'MUSICA', 'Álbum recopilatorio de Judas Priest en formato CD.', 'ACTIVO', 'CD', 'Heavy Metal', 'http://localhost:5173/productos/musica/judas.jpg', 'Judas Priest — The Best of Judas Priest (1 CD)', NULL, 10560.00),
(5, 'MUSICA', 'Álbum de Machine Head en formato CD.', 'ACTIVO', 'Fisico', 'Groove Metal', 'http://localhost:5173/productos/musica/machine.jpg', 'Machine Head — Through the Ashes of Empires (1 CD)', NULL, 7590.00),
(6, 'MUSICA', 'Álbum Heaven Up Here de Echo & The Bunnymen en formato CD.', 'ACTIVO', 'Físico', 'Post-punk', 'http://localhost:5173/productos/musica/bun.jpg', 'Echo & The Bunnymen — Heaven Up Here (1 CD)', NULL, 17490.00),
(7, 'MUSICA', 'The Anthology de A Tribe Called Quest, edición en CD.', 'ACTIVO', 'CD', 'Hip-Hop', 'http://localhost:5173/productos/musica/a-tribe-called-quest-the-anthology.jpg', 'A Tribe Called Quest — The Anthology (CD)', NULL, 17990.00),
(8, 'MUSICA', 'Maningen de Atarashii Gakko, edición en CD.', 'ACTIVO', 'CD', 'J-Pop', 'http://localhost:5173/productos/musica/atarashii-gakko-maningen.jpg', 'Atarashii Gakko — Maningen (CD)', NULL, 24990.00),
(9, 'MUSICA', 'Mask de Bauhaus, edición en vinilo.', 'ACTIVO', 'Vinilo', 'Gothic Rock', 'http://localhost:5173/productos/musica/bauhaus-mask.jpg', 'Bauhaus — Mask (Vinilo)', NULL, 33990.00),
(10, 'MUSICA', 'Álbum Post Human: Survival Horror de Bring Me The Horizon, edición en CD.', 'ACTIVO', 'CD', 'Metalcore', 'http://localhost:5173/productos/musica/bring-me-the-horizon-post-human-survival-horror.jpg', 'Bring Me The Horizon — Post Human: Survival Horror (CD)', NULL, 17990.00),
(11, 'MUSICA', 'Álbum Suicide Season de Bring Me The Horizon, edición en CD.', 'ACTIVO', 'CD', 'Metalcore', 'http://localhost:5173/productos/musica/bring-me-the-horizon-suicide-season.jpg', 'Bring Me The Horizon — Suicide Season (CD)', NULL, 15990.00),
(12, 'MUSICA', 'The Money Store de Death Grips, edición en vinilo.', 'ACTIVO', 'Vinilo', 'Experimental Hip-Hop', 'http://localhost:5173/productos/musica/death-grips-the-money-store.jpg', 'Death Grips — The Money Store (Vinilo)', NULL, 34990.00),
(13, 'MUSICA', 'Petals For Armor de Hayley Williams, edición de vinilo doble.', 'ACTIVO', '2LP', 'Alternative Pop', 'http://localhost:5173/productos/musica/hayley-williams-petals-for-armor.jpg', 'Hayley Williams — Petals For Armor (Vinilo doble)', NULL, 44990.00),
(14, 'MUSICA', 'Lost In The Waves: The Complete Edition de Landmvrks, edición de dos CD.', 'ACTIVO', '2CD', 'Metalcore', 'http://localhost:5173/productos/musica/landmvrks-lost-in-the-waves.jpg', 'Landmvrks — Lost In The Waves: The Complete Edition (CD doble)', NULL, 26990.00),
(15, 'MUSICA', 'Facades de Limp Wrist, edición en vinilo.', 'ACTIVO', 'Vinilo', 'Hardcore Punk', 'http://localhost:5173/productos/musica/limp-wrist-facades.jpg', 'Limp Wrist — Facades (Vinilo)', NULL, 29990.00),
(16, 'MUSICA', 'Forever Live Sessions Vol. 2 de Men I Trust en vinilo.', 'ACTIVO', 'Vinilo', 'Indie Pop', 'http://localhost:5173/productos/musica/men-i-trust-forever-live-sessions-vol-2.jpg', 'Men I Trust — Forever Live Sessions Vol. 2 (Vinilo)', NULL, 34990.00),
(17, 'MUSICA', 'Álbum Off The Wall de Michael Jackson en vinilo.', 'ACTIVO', 'Vinilo', 'Pop', 'http://localhost:5173/productos/musica/michael-jackson-off-the-wall.jpg', 'Michael Jackson — Off The Wall (Vinilo)', NULL, 32990.00),
(18, 'MUSICA', 'Day The Earth Caught Fire de Misfits / Balzac, edición en CD.', 'ACTIVO', 'CD', 'Horror Punk', 'http://localhost:5173/productos/musica/misfits-balzac-day-the-earth-caught-fire.jpg', 'Misfits / Balzac — Day The Earth Caught Fire (CD)', NULL, 19990.00),
(19, 'MUSICA', 'Antithese de My First Story. Formato CD provisional para este catálogo de prueba.', 'ACTIVO', 'CD', 'J-Rock', 'http://localhost:5173/productos/musica/my-first-story-antithese.jpg', 'My First Story — Antithese (CD)', NULL, 22990.00),
(20, 'MUSICA', 'Dance On The Blacktop de Nothing, edición de vinilo Black Ice.', 'ACTIVO', 'Vinilo', 'Shoegaze', 'http://localhost:5173/productos/musica/nothing-dance-on-the-blacktop.jpg', 'Nothing — Dance On The Blacktop (Black Ice) (Vinilo)', NULL, 36990.00),
(21, 'MUSICA', 'Crooked Teeth de Papa Roach, edición en CD.', 'ACTIVO', 'CD', 'Alternative Metal', 'http://localhost:5173/productos/musica/papa-roach-crooked-teeth.jpg', 'Papa Roach — Crooked Teeth (CD)', NULL, 15990.00),
(22, 'MUSICA', 'Deadwing de Porcupine Tree, edición de vinilo doble.', 'ACTIVO', '2LP', 'Progressive Rock', 'http://localhost:5173/productos/musica/porcupine-tree-deadwing.jpg', 'Porcupine Tree — Deadwing (Vinilo doble)', NULL, 46990.00),
(23, 'MUSICA', 'Antipop de Primus, edición de vinilo doble.', 'ACTIVO', '2LP', 'Alternative Rock', 'http://localhost:5173/productos/musica/primus-antipop.jpg', 'Primus — Antipop (Vinilo doble)', NULL, 42990.00),
(24, 'MUSICA', '\"V\" Is For Vagina de Puscifer; edición de vinilo doble Blue with Black Smoke.', 'ACTIVO', '2LP', 'Alternative Rock', 'http://localhost:5173/productos/musica/puscifer-v-is-for-vagina.jpg', 'Puscifer — \"V\" Is For Vagina (Blue with Black Smoke) (Vinilo doble)', NULL, 49990.00),
(25, 'MUSICA', 'Next To Die de Six Feet Under, edición en CD; verificar denominación exacta del lanzamiento.', 'ACTIVO', 'CD', 'Death Metal', 'http://localhost:5173/productos/musica/six-feet-under-next-to-die.jpg', 'Six Feet Under — Next To Die (CD)', NULL, 16990.00),
(26, 'MUSICA', 'Saudade de Thievery Corporation, edición en CD.', 'ACTIVO', 'CD', 'Downtempo', 'http://localhost:5173/productos/musica/thievery-corporation-saudade.jpg', 'Thievery Corporation — Saudade (CD)', NULL, 17990.00),
(38, 'SERIES_PELICULAS_ANIME', 'La malvada Cruella de Vil desea apoderarse de unos cachorros dálmatas, pero sus familias harán todo lo posible para rescatarlos. Clásico Disney.', 'ACTIVO', 'DVD', 'Película', '/productos/catalogo/101-dalmatas-disney-b02c4e42.webp', '101 Dálmatas (Disney)', NULL, 14990.00),
(39, 'SERIES_PELICULAS_ANIME', 'Drama romántico ambientado en Kenia que relata el vínculo entre Karen Blixen y Denys Finch Hatton. Cine clásico.', 'ACTIVO', 'DVD', 'Película', '/productos/catalogo/frica-mia-100f6063.jpg', 'África Mía', NULL, 14990.00),
(40, 'ACCESORIOS', 'Amplificador Behringer AMP800 V2 de cuatro canales para distribuir y ajustar señales de monitoreo por auriculares.', 'ACTIVO', 'Físico', 'Audio / Tecnología', '/productos/catalogo/amplificador-de-auriculares-behringer-amp800-v2-de-4-canales-2580deb8.jpg', 'Amplificador de auriculares Behringer AMP800 V2 de 4 canales', NULL, 29990.00),
(41, 'ACCESORIOS', 'Amplificador de monitoreo personal Behringer P1, pensado para controlar una mezcla de audio escuchada mediante auriculares.', 'ACTIVO', 'Físico', 'Audio / Tecnología', '/productos/catalogo/amplificador-de-monitoreo-personal-behringer-p1-a9ac5a17.jpg', 'Amplificador de monitoreo personal Behringer P1', NULL, 29990.00),
(42, 'SERIES_PELICULAS_ANIME', 'Película animada sobre una joven que intenta descubrir su pasado mientras emprende un viaje para encontrar a su familia.', 'ACTIVO', 'DVD', 'Película', '/productos/catalogo/anastasia-1d48c56b.jpg', 'Anastasia', NULL, 14990.00),
(43, 'SERIES_PELICULAS_ANIME', 'Scott Lang y Hope van Dyne emprenden una nueva misión utilizando tecnología que les permite cambiar de tamaño. Aventura de Marvel.', 'ACTIVO', 'Blu-ray', 'Película', '/productos/catalogo/ant-man-and-the-wasp-04742908.jpg', 'Ant-Man and The Wasp', NULL, 19990.00),
(44, 'SERIES_PELICULAS_ANIME', 'Arthur Curry debe aceptar su herencia atlante y enfrentar un conflicto entre el mundo submarino y la superficie. Edición Steelbook indicada en el título.', 'ACTIVO', 'Blu-ray', 'Película', '/productos/catalogo/aquaman-edicion-steelbook-bfdf9dff.jpg', 'Aquaman (Edición Steelbook)', NULL, 19990.00),
(45, 'SERIES_PELICULAS_ANIME', 'Colección de la serie de televisión Área 12 en presentación de seis DVD y con audio en español latino indicado en el título.', 'ACTIVO', 'DVD', 'Serie', '/productos/catalogo/area-12-serie-de-tv-espanol-latino-6-dvd-e03de25f.webp', 'Área 12 - Serie de TV Español Latino (6 DVD)', NULL, 21990.00),
(46, 'SERIES_PELICULAS_ANIME', 'Anime clásico de competición automovilística sobre pilotos, rivalidades y carreras exigentes. Presentación Blu-ray Box custom.', 'ACTIVO', 'Blu-ray', 'Anime', '/productos/catalogo/arrow-emblem-grand-prix-no-taka-blu-ray-box-custom-5cf58d8e.jpg', 'Arrow Emblem Grand Prix no Taka (Blu-ray Box Custom)', NULL, 24990.00),
(47, 'SERIES_PELICULAS_ANIME', 'Misterio policial basado en la novela de Agatha Christie: el detective Hércules Poirot investiga un asesinato durante un viaje en tren.', 'ACTIVO', 'Blu-ray', 'Película', '/productos/catalogo/asesinato-en-el-expreso-de-oriente-d876c022.jpg', 'Asesinato en el Expreso de Oriente', NULL, 19990.00),
(48, 'SERIES_PELICULAS_ANIME', 'Callum Lynch descubre un vínculo con una antigua hermandad de asesinos mediante una tecnología capaz de explorar recuerdos ancestrales.', 'ACTIVO', 'Blu-ray', 'Película', '/productos/catalogo/assasins-creed-934bc746.jpg', 'Assassin’s Creed', NULL, 19990.00),
(49, 'ACCESORIOS', 'Audífonos de diseño cerrado para escuchar, grabar o monitorear audio con mayor aislamiento respecto del sonido ambiental.', 'ACTIVO', 'Físico', 'Audio / Tecnología', '/productos/catalogo/audifonos-cerrados-para-estudio-5433069d.jpg', 'Audífonos cerrados para estudio', NULL, 29990.00),
(50, 'SERIES_PELICULAS_ANIME', 'Los Vengadores y sus aliados se enfrentan a Thanos, decidido a reunir las Gemas del Infinito. Aventura del Universo Cinematográfico de Marvel.', 'ACTIVO', 'DVD', 'Película', '/productos/catalogo/avengers-infinity-war-7e31cfc4.jpg', 'Avengers: Infinity War (Marvel)', NULL, 14990.00),
(51, 'SERIES_PELICULAS_ANIME', 'Las aventuras del joven Ben Tennyson, que utiliza el Omnitrix para transformarse en héroes alienígenas y proteger a quienes lo rodean. Colección de Ben 10 clásico en DVD.', 'ACTIVO', 'DVD', 'Serie', '/productos/catalogo/ben-10-classic-coleccion-completa-warner-bros-dvd-732a354c.jpg', 'Ben 10 Classic - Colección Completa Warner Bros (DVD)', NULL, 18990.00),
(52, 'SERIES_PELICULAS_ANIME', 'Una colonia de hormigas decide pedir ayuda para defenderse de los saltamontes. Aventura animada de Disney y Pixar.', 'ACTIVO', 'DVD', 'Película', '/productos/catalogo/bichos-una-aventura-en-miniatura-disney-7bd2d641.jpg', 'Bichos: Una Aventura en Miniatura (Disney)', NULL, 14990.00),
(53, 'SERIES_PELICULAS_ANIME', 'Colección de Bleach centrada en las aventuras de Ichigo Kurosaki y el mundo de los shinigami; incluye material adicional indicado en el título.', 'ACTIVO', 'DVD', 'Anime', '/productos/catalogo/bleach-serie-tv-vol-1-366-peliculas-especiales-dvd-1b766f79.jpg', 'Bleach - Serie TV Vol. 1-366 + Películas + Especiales (DVD)', NULL, 24990.00),
(54, 'MERCHANDISING', 'Botella termo de 530 ml con temática de Hogwarts y Harry Potter, pensada para uso diario.', 'ACTIVO', 'Físico', 'Coleccionable', '/productos/catalogo/botella-termo-hogwarts-530-ml-harry-potter-8c79573e.jpg', 'Botella Termo Hogwarts 530 ml - Harry Potter', NULL, 12990.00),
(55, 'SERIES_PELICULAS_ANIME', 'Tercera temporada de la serie Breaking Bad, con Walter White y Jesse Pinkman enfrentando consecuencias cada vez mayores en el mundo del narcotráfico.', 'ACTIVO', 'DVD', 'Película', '/productos/catalogo/breaking-bad-temporada-3-76c759ba.jpg', 'Breaking Bad - Temporada 3', NULL, 18990.00),
(56, 'MERCHANDISING', 'Colección especial de cartas Pokémon centrada en Charizard ex. Presentación en español para jugadores y coleccionistas.', 'ACTIVO', 'Físico', 'Coleccionable', '/productos/catalogo/charizard-ex-special-collection-espanol-6edfaa53.png', 'Charizard EX Special Collection - Español', NULL, 12990.00),
(57, 'SERIES_PELICULAS_ANIME', 'Christopher Robin se reencuentra con Winnie the Pooh y sus amigos del Bosque de los Cien Acres. Fantasía familiar.', 'ACTIVO', 'Blu-ray', 'Película', '/productos/catalogo/christopher-robin-un-reencuentro-inolvidable-09a06e4d.jpg', 'Christopher Robin: Un Reencuentro Inolvidable', NULL, 19990.00),
(58, 'VIDEOJUEGOS', 'Nueva entrega de la serie Coffee Talk, centrada en conversaciones, historias personales y la preparación de bebidas. Juego para PS5.', 'ACTIVO', 'Físico', 'Videojuego', '/productos/catalogo/coffee-talk-tokyo-ff13261a.jpg', 'Coffee Talk Tokyo (PS5)', 'PS5', 39990.00),
(59, 'SERIES_PELICULAS_ANIME', 'John Constantine investiga amenazas sobrenaturales mientras lucha con sus propios demonios. Película inspirada en los cómics de DC.', 'ACTIVO', 'Blu-ray', 'Película', '/productos/catalogo/constantine-28f0435e.jpg', 'Constantine', NULL, 19990.00),
(60, 'SERIES_PELICULAS_ANIME', 'Un joven escudero desafía las reglas de su época para convertirse en caballero y competir en torneos medievales.', 'ACTIVO', 'Blu-ray', 'Película', '/productos/catalogo/corazon-de-caballero-a54b5108.jpg', 'Corazón de Caballero', NULL, 19990.00),
(61, 'VIDEOJUEGOS', 'Aventura de terror y misterio de la franquicia Corpse Party, con exploración y una historia de suspenso. Juego para PS5.', 'ACTIVO', 'Físico', 'Videojuego', '/productos/catalogo/corpse-party-2-darkness-distortion-7c611833.jpg', 'Corpse Party 2: Darkness Distortion (PS5)', 'PS5', 39990.00),
(62, 'VIDEOJUEGOS', 'RPG de acción que sigue a Zack Fair y amplía la historia del universo Final Fantasy VII. Edición para PS5.', 'ACTIVO', 'Físico', 'Videojuego', '/productos/catalogo/crisis-core-final-fantasy-vii-reunion-ea85c36b.jpg', 'Crisis Core: Final Fantasy VII Reunion (PS5)', 'PS5', 39990.00),
(63, 'VIDEOJUEGOS', 'Edición para PS5 de Culdcept The First Saturn Tribute, mezcla de estrategia de tablero y cartas coleccionables.', 'ACTIVO', 'Físico', 'Videojuego', '/productos/catalogo/culdcept-the-first-saturn-tribute-7a5cb7d8.jpg', 'Culdcept The First Saturn Tribute (PS5)', 'PS5', 39990.00),
(64, 'VIDEOJUEGOS', 'RPG de acción de mundo abierto ambientado en Night City. Ultimate Edition para PS5, con la experiencia principal y contenido adicional de la edición.', 'ACTIVO', 'Físico', 'Videojuego', '/productos/catalogo/cyberpunk-2077-ultimate-edition-fc28da4d.jpg', 'Cyberpunk 2077: Ultimate Edition (PS5)', 'PS5', 39990.00),
(65, 'VIDEOJUEGOS', 'Edición física del juego de acción roguelite Dead Cells vinculada al universo de Castlevania. Juego para PS5.', 'ACTIVO', 'Físico', 'Videojuego', '/productos/catalogo/dead-cells-return-to-castlevania-edition-f1bce6a9.jpg', 'Dead Cells: Return to Castlevania Edition (PS5)', 'PS5', 39990.00),
(66, 'VIDEOJUEGOS', 'Continuación de Death Stranding, con Sam emprendiendo una nueva misión en un mundo transformado. Juego para PS5.', 'ACTIVO', 'Físico', 'Videojuego', '/productos/catalogo/death-stranding-2-on-the-beach-3a88f3b4.jpg', 'Death Stranding 2: On The Beach (PS5)', 'PS5', 39990.00),
(67, 'VIDEOJUEGOS', 'Versión Director’s Cut de la aventura de Sam Porter Bridges, centrada en conectar comunidades tras un fenómeno devastador. Juego para PS5.', 'ACTIVO', 'Físico', 'Videojuego', '/productos/catalogo/death-stranding-9bd1acd5.jpg', 'Death Stranding Director’s Cut (PS5)', 'PS5', 39990.00),
(68, 'VIDEOJUEGOS', 'Juego para PS5 titulado Demons Night Fever. Presentación física para coleccionistas de videojuegos.', 'ACTIVO', 'Físico', 'Videojuego', '/productos/catalogo/demons-night-fever-caba8a58.jpg', 'Demons Night Fever (PS5)', 'PS5', 39990.00),
(69, 'VIDEOJUEGOS', 'Remake del desafiante RPG de acción y fantasía oscura de FromSoftware, ambientado en el reino de Boletaria. Juego para PS5.', 'ACTIVO', 'Físico', 'Videojuego', '/productos/catalogo/demons-souls-9f6b4451.jpeg', 'Demon’s Souls (PS5)', 'PS5', 39990.00),
(70, 'MERCHANDISING', 'Destapador de botellas con diseño inspirado en el Halcón Milenario de Star Wars.', 'ACTIVO', 'Físico', 'Coleccionable', '/productos/catalogo/destapador-halcon-milenario-692c7533.jpg', 'Destapador Halcón Milenario', NULL, 12990.00),
(71, 'VIDEOJUEGOS', 'RPG de acción de Blizzard en el mundo de Santuario, con clases, misiones, botín y combates contra fuerzas demoníacas. Juego para PS5.', 'ACTIVO', 'Físico', 'Videojuego', '/productos/catalogo/diablo-iv-c55bcab0.jpg', 'Diablo IV (PS5)', 'PS5', 39990.00),
(72, 'VIDEOJUEGOS', 'RPG del universo Digimon con aventuras y combates junto a criaturas digitales. Versión europea para PS5.', 'ACTIVO', 'Físico', 'Videojuego', '/productos/catalogo/digimon-story-time-stranger-a38f9021.jpg', 'Digimon Story Time Stranger (Europeo, PS5)', 'PS5', 39990.00),
(73, 'VIDEOJUEGOS', 'Juego de estrategia y rol de fantasía oscura para PC, con facciones, combates por turnos y desarrollo de unidades.', 'ACTIVO', 'Físico', 'Videojuego', '/productos/catalogo/disciples-iii-7640f8bc.jpg', 'Disciples III (PC físico)', 'PC', 39990.00),
(74, 'VIDEOJUEGOS', 'Juego de combate y aventura en 3D del universo Dragon Ball, con creación de personajes y enfrentamientos entre guerreros. Versión PS5 indicada en el catálogo.', 'ACTIVO', 'Físico', 'Videojuego', '/productos/catalogo/dragon-ball-xenoverse-2-c1f08175.png', 'Dragon Ball Xenoverse 2 (PS5)', 'PS5', 39990.00),
(75, 'SERIES_PELICULAS_ANIME', 'Pack doble con dos enfrentamientos de ciencia ficción entre las criaturas Alien y Predator.', 'ACTIVO', 'Blu-ray', 'Película', '/productos/catalogo/duo-alien-vs-depredador-1-y-2-61ab8e98.jpg', 'Dúo Alien vs Depredador 1 y 2', NULL, 19990.00),
(76, 'SERIES_PELICULAS_ANIME', 'Colección doble de ciencia ficción con la invasión extraterrestre de Día de la Independencia y su continuación Contraataque.', 'ACTIVO', 'Blu-ray', 'Película', '/productos/catalogo/duo-dia-de-la-independencia-contraataque-c3ddc15a.jpg', 'Dúo Día de la Independencia / Contraataque', NULL, 19990.00),
(77, 'SERIES_PELICULAS_ANIME', 'Sheeta y Pazu buscan los secretos de una legendaria isla flotante mientras son perseguidos por piratas y agentes del gobierno. Película de Studio Ghibli.', 'ACTIVO', 'DVD', 'Película', '/productos/catalogo/el-castillo-en-el-cielo-ghibli-miyazaki-a375f588.jpg', 'El Castillo en el Cielo (Ghibli Miyazaki)', NULL, 14990.00),
(78, 'SERIES_PELICULAS_ANIME', 'Un hombre desencantado conoce a Tyler Durden y se ve envuelto en un club clandestino que cambia su vida. Thriller dirigido por David Fincher.', 'ACTIVO', 'Blu-ray', 'Película', '/productos/catalogo/el-club-de-la-pelea-8041d780.jpg', 'El Club de la Pelea', NULL, 19990.00),
(79, 'SERIES_PELICULAS_ANIME', 'Edward Bloom relata aventuras extraordinarias de su vida mientras su hijo intenta descubrir qué hay de verdad en sus historias. Fantasía dirigida por Tim Burton.', 'ACTIVO', 'DVD', 'Película', '/productos/catalogo/el-gran-pez-tim-burton-8b6e9c38.webp', 'El Gran Pez (Tim Burton)', NULL, 14990.00),
(80, 'SERIES_PELICULAS_ANIME', 'Anna llega a una tranquila localidad costera y conoce a la misteriosa Marnie. Historia de amistad y recuerdos de Studio Ghibli.', 'ACTIVO', 'DVD', 'Película', '/productos/catalogo/el-recuerdo-de-marnie-ghibli-miyazaki-77d48bc0.jpg', 'El Recuerdo de Marnie (Ghibli Miyazaki)', NULL, 14990.00),
(81, 'VIDEOJUEGOS', 'RPG de acción de fantasía en un vasto mundo de exploración y combates exigentes. Edición Tarnished anunciada para Nintendo Switch 2.', 'ACTIVO', 'Físico', 'Videojuego', '/productos/catalogo/elden-ring-tarnished-edition-e640cf03.jpg', 'Elden Ring Tarnished Edition (Switch 2)', 'Switch 2', 39990.00),
(82, 'MERCHANDISING', 'Estuche para Nintendo Switch con diseño inspirado en una Poké Ball, pensado para transportar y proteger la consola.', 'ACTIVO', 'Físico', 'Coleccionable', '/productos/catalogo/estuche-nintendo-switch-poke-ball-d7e6bd69.jpg', 'Estuche Nintendo Switch Poké Ball', NULL, 12990.00),
(83, 'VIDEOJUEGOS', 'Juego de rol táctico de la saga Final Fantasy, con batallas estratégicas y un relato de conflictos políticos. Edición The Ivalice Chronicles para PS5.', 'ACTIVO', 'Físico', 'Videojuego', '/productos/catalogo/final-fantasy-tactics-the-ivalice-chronicles-873738ce.jpg', 'Final Fantasy Tactics: The Ivalice Chronicles (PS5)', 'PS5', 39990.00),
(84, 'SERIES_PELICULAS_ANIME', 'Colección física de Fullmetal Alchemist con temporadas, películas y OVA indicados en el título. Una saga de aventura, alquimia y vínculos familiares.', 'ACTIVO', 'DVD', 'Anime', '/productos/catalogo/fullmetal-alchemist-temporadas-1-y-2-peliculas-ova-dvd-a3b3ac4b.jpg', 'Fullmetal Alchemist - Temporadas 1 y 2 + Películas + OVA (DVD)', NULL, 24990.00),
(85, 'SERIES_PELICULAS_ANIME', 'Las aventuras de Fry, Leela y Bender en un futuro lleno de humor, ciencia ficción y viajes espaciales. Serie animada en DVD.', 'ACTIVO', 'DVD', 'Serie', '/productos/catalogo/futurama-serie-original-en-dvd-0a257cd4.webp', 'Futurama - Serie Original en DVD', NULL, 21990.00),
(86, 'SERIES_PELICULAS_ANIME', 'Clásico del anime cyberpunk sobre identidad, conciencia y tecnología en una sociedad conectada. Película de ciencia ficción.', 'ACTIVO', 'DVD', 'Anime', '/productos/catalogo/ghost-in-the-shell-f9eb1956.jpg', 'Ghost in the Shell', NULL, 24990.00),
(87, 'SERIES_PELICULAS_ANIME', 'Serie juvenil dramática sobre un grupo de jóvenes de la alta sociedad neoyorquina, sus romances, rivalidades y secretos. Colección completa en DVD.', 'ACTIVO', 'DVD', 'Serie', '/productos/catalogo/gossip-girl-the-complete-series-dvd-8d079205.webp', 'Gossip Girl - The Complete Series (DVD)', NULL, 21990.00),
(88, 'SERIES_PELICULAS_ANIME', 'Segunda etapa de las aventuras de Ippo Makunouchi, un joven boxeador que sigue creciendo entre entrenamientos y combates. Edición custom en Blu-ray.', 'ACTIVO', 'Blu-ray', 'Anime', '/productos/catalogo/hajime-no-ippo-new-challenger-44572626.jpg', 'Hajime no Ippo: New Challenger (Blu-ray Custom)', NULL, 24990.00),
(89, 'SERIES_PELICULAS_ANIME', 'Dos agentes secretos supervisan la presencia extraterrestre en la Tierra mientras investigan amenazas fuera de este mundo.', 'ACTIVO', 'Blu-ray', 'Película', '/productos/catalogo/hombres-de-negro-2c29226b.jpg', 'Hombres de Negro', NULL, 19990.00),
(90, 'SERIES_PELICULAS_ANIME', 'Colección de las dos primeras aventuras de los agentes encargados de vigilar la actividad extraterrestre en la Tierra.', 'ACTIVO', 'Blu-ray', 'Película', '/productos/catalogo/hombres-de-negro-1-y-2-aa4f0028.jpg', 'Hombres de Negro 1 y 2', NULL, 19990.00),
(91, 'SERIES_PELICULAS_ANIME', 'Horton, un elefante, descubre un pequeño mundo sobre una mota de polvo y se empeña en proteger a sus habitantes. Animación familiar.', 'ACTIVO', 'DVD', 'Película', '/productos/catalogo/horton-y-el-mundo-de-los-quien-c83e064f.jpg', 'Horton y el Mundo de los Quién', NULL, 14990.00),
(92, 'SERIES_PELICULAS_ANIME', 'Banda sonora original de Infinite Stratos. CD con música asociada a la serie de anime, pensado para coleccionistas de soundtracks.', 'ACTIVO', 'CD', 'Anime', '/productos/catalogo/infinite-stratos-original-soundtrack-938355d6.jpg', 'Infinite Stratos - Original Soundtrack', NULL, 16990.00),
(93, 'SERIES_PELICULAS_ANIME', 'Serie animada de Iron Man de 1994, con Tony Stark enfrentando amenazas tecnológicas y supervillanos. Edición usada indicada en el título, en tres DVD.', 'ACTIVO', 'DVD', 'Serie', '/productos/catalogo/iron-man-serie-animada-completa-de-1994-3-dvd-usado-ca1d904f.webp', 'Iron Man - Serie Animada Completa de 1994 (3 DVD, usado)', NULL, 21990.00),
(94, 'SERIES_PELICULAS_ANIME', 'Anime clásico de ciencia ficción protagonizado por Jet Marte, un joven robot que aprende sobre la humanidad. Presentación SD on BD.', 'ACTIVO', 'Blu-ray', 'Anime', '/productos/catalogo/jet-marte-sd-on-bd-blu-ray-ce390967.jpg', 'Jet Marte (SD on BD Blu-ray)', NULL, 24990.00),
(95, 'SERIES_PELICULAS_ANIME', 'Colección de las cuatro primeras películas de John Wick, un asesino retirado arrastrado de nuevo al mundo del crimen.', 'ACTIVO', 'Blu-ray', 'Película', '/productos/catalogo/john-wick-capitulos-1-4-802ebfd2.jpg', 'John Wick Capítulos 1-4', NULL, 19990.00),
(96, 'MERCHANDISING', 'Polera inspirada en Jiji, el gato de Kiki: Entregas a Domicilio, película de Studio Ghibli.', 'ACTIVO', 'Físico', 'Coleccionable', '/productos/catalogo/kiki-delivery-service-jiji-polera-cb6dbe74.jpg', 'Kiki Delivery Service - Jiji - Polera', NULL, 12990.00),
(97, 'SERIES_PELICULAS_ANIME', 'La Novia retoma su búsqueda de venganza y se acerca al enfrentamiento final con Bill. Segunda parte de la historia de Quentin Tarantino.', 'ACTIVO', 'DVD', 'Película', '/productos/catalogo/kill-bill-volumen-2-tarantino-aadb764d.jpg', 'Kill Bill Volumen 2 (Tarantino)', NULL, 14990.00),
(98, 'SERIES_PELICULAS_ANIME', 'Sigue las misiones de Kim Possible, una estudiante que combina la vida escolar con aventuras de espionaje y acción. Serie animada clásica.', 'ACTIVO', 'DVD', 'Serie', '/productos/catalogo/kim-possible-la-serie-animada-clasica-9f9726c6.jpg', 'Kim Possible - La Serie Animada Clásica', NULL, 18990.00),
(99, 'SERIES_PELICULAS_ANIME', 'Cuento animado de Disney sobre la princesa Aurora, una maldición y el enfrentamiento entre las fuerzas del bien y Maléfica.', 'ACTIVO', 'DVD', 'Película', '/productos/catalogo/la-bella-durmiente-disney-535954f4.jpg', 'La Bella Durmiente (Disney)', NULL, 14990.00),
(100, 'SERIES_PELICULAS_ANIME', 'Clásico de terror y ciencia ficción sobre una expedición antártica amenazada por una criatura capaz de imitar a cualquier ser vivo.', 'ACTIVO', 'Blu-ray', 'Película', '/productos/catalogo/la-cosa-the-thing-ed855479.jpg', 'La Cosa (The Thing)', NULL, 19990.00),
(101, 'SERIES_PELICULAS_ANIME', 'Serie antológica clásica de ciencia ficción, suspenso y situaciones extraordinarias, conocida por sus relatos de giros inesperados. Colección de La Dimensión Desconocida.', 'ACTIVO', 'DVD', 'Serie', '/productos/catalogo/la-dimension-desconocida-serie-de-tv-paramount-completa-c9a8e608.avif', 'La Dimensión Desconocida - Serie de TV Paramount Completa', NULL, 21990.00),
(102, 'SERIES_PELICULAS_ANIME', 'Thriller que recrea la prolongada investigación para localizar a Osama bin Laden y las decisiones de quienes participan en ella.', 'ACTIVO', 'Blu-ray', 'Película', '/productos/catalogo/la-noche-mas-oscura-zero-dark-thirty-a409433f.jpg', 'La Noche Más Oscura (Zero Dark Thirty)', NULL, 19990.00),
(103, 'SERIES_PELICULAS_ANIME', 'Hal Jordan recibe un anillo que le otorga poderes extraordinarios y lo incorpora a un cuerpo intergaláctico de guardianes.', 'ACTIVO', 'Blu-ray', 'Película', '/productos/catalogo/linterna-verde-1b9ac8b8.jpg', 'Linterna Verde', NULL, 19990.00),
(104, 'MERCHANDISING', 'Llavero inspirado en One Piece para llevar llaves o personalizar una mochila con motivos de la serie.', 'ACTIVO', 'Físico', 'Coleccionable', '/productos/catalogo/llavero-keychain-one-piece-6e09b3e6.jpg', 'Llavero / Keychain - One Piece', NULL, 12990.00),
(105, 'SERIES_PELICULAS_ANIME', 'Drama clásico sobre Scarlett O’Hara y los cambios que afronta durante la Guerra de Secesión estadounidense. Presentación en DVD.', 'ACTIVO', 'DVD', 'Serie', '/productos/catalogo/lo-que-el-viento-se-llevo-dvd-original-0917cdb4.jpg', 'Lo Que el Viento Se Llevó - DVD Original', NULL, 14990.00),
(106, 'SERIES_PELICULAS_ANIME', 'Anime de misterio y fantasía sobre libros sobrenaturales y secretos peligrosos, protagonizado por Dalian y Hugh Anthony Disward. Colección de la serie.', 'ACTIVO', 'DVD', 'Serie', '/productos/catalogo/los-archivos-misticos-de-dantalian-la-serie-completa-e042626d.jpg', 'Los Archivos Místicos de Dantalian - La Serie Completa', NULL, 18990.00),
(107, 'MERCHANDISING', 'Paquete Booster Bundle de la expansión Mega Evolution del juego de cartas Pokémon. Producto en inglés para ampliar una colección.', 'ACTIVO', 'Físico', 'Coleccionable', '/productos/catalogo/mega-evolution-base-set-booster-bundle-ingles-fccb830b.jpg', 'Mega Evolution (Base Set) Booster Bundle - Inglés', NULL, 12990.00),
(108, 'MERCHANDISING', 'Elite Trainer Box de la expansión Mega Evolution Chaos Rising del juego de cartas Pokémon, en inglés.', 'ACTIVO', 'Físico', 'Coleccionable', '/productos/catalogo/mega-evolution-chaos-rising-elite-trainer-box-ingles-63049ad5.jpg', 'Mega Evolution Chaos Rising Elite Trainer Box - Inglés', NULL, 12990.00),
(109, 'SERIES_PELICULAS_ANIME', 'Antología de cortometrajes de animación japonesa que reúne historias de distintos estilos, desde la ciencia ficción hasta el drama.', 'ACTIVO', 'DVD', 'Anime', '/productos/catalogo/memories-fd5c9879.jpg', 'Memories', NULL, 24990.00),
(110, 'SERIES_PELICULAS_ANIME', 'Musical clásico en el que el profesor Henry Higgins intenta transformar la forma de hablar y desenvolverse de Eliza Doolittle.', 'ACTIVO', 'DVD', 'Película', '/productos/catalogo/mi-bella-dama-my-fair-lady-b3c4babe.jpg', 'Mi Bella Dama (My Fair Lady)', NULL, 14990.00),
(111, 'ACCESORIOS', 'Monitor de estudio activo KRK V8 Series Five con altavoz de 8 pulgadas. Diseñado para tareas de escucha crítica y producción musical.', 'ACTIVO', 'Físico', 'Audio / Tecnología', '/productos/catalogo/monitor-de-estudio-activo-krk-v8-series-five-8-pulgadas-455-w-a966efde.jpg', 'Monitor de estudio activo KRK V8 Series Five 8 pulgadas 455 W', NULL, 29990.00),
(112, 'MERCHANDISING', 'Polera de One Piece con diseño de Roronoa Zoro en estilo de viñetas. Prenda para seguidores del anime y manga.', 'ACTIVO', 'Físico', 'Coleccionable', '/productos/catalogo/one-piece-zoro-vinetas-polera-20a33077.jpg', 'One Piece - Zoro Viñetas - Polera', NULL, 12990.00),
(113, 'SERIES_PELICULAS_ANIME', 'Drama de espionaje en el que una exbailarina es reclutada para una misión de inteligencia llena de engaños y peligros.', 'ACTIVO', 'DVD', 'Película', '/productos/catalogo/operacion-red-sparrow-c081b973.jpg', 'Operación Red Sparrow', NULL, 14990.00),
(114, 'MERCHANDISING', 'Adorno tipo bag charm inspirado en Orb: On the Movements of the Earth, pensado para decorar bolsos o mochilas.', 'ACTIVO', 'Físico', 'Coleccionable', '/productos/catalogo/orb-on-the-movements-of-the-earth-bag-charm-c6cc44be.jpg', 'Orb: On the Movements of the Earth - Bag Charm', NULL, 12990.00),
(115, 'SERIES_PELICULAS_ANIME', 'OVA de ciencia ficción y comedia romántica en la que una joven alienígena llega a la Tierra y desencadena una aventura inesperada. Edición Blu-ray.', 'ACTIVO', 'Blu-ray', 'Anime', '/productos/catalogo/outlanders-blu-ray-14d8cd88.jpg', 'Outlanders (Blu-ray)', NULL, 24990.00),
(116, 'SERIES_PELICULAS_ANIME', 'Nuevo proyecto de la franquicia Patlabor, centrado en equipos policiales que operan robots de trabajo. Lanzamiento File-1 en Blu-ray.', 'ACTIVO', 'Blu-ray', 'Anime', '/productos/catalogo/patlabor-ezy-file-1-blu-ray-21f83267.jpg', 'Patlabor EZY File-1 (Blu-ray)', NULL, 24990.00),
(117, 'VIDEOJUEGOS', 'Juego de ciencia ficción de Capcom con exploración y acción en un entorno futurista. Título para PS5.', 'ACTIVO', 'Físico', 'Videojuego', '/productos/catalogo/pragmata-934f0f2f.jpg', 'Pragmata (PS5)', 'PS5', 39990.00),
(118, 'SERIES_PELICULAS_ANIME', 'Miniálbum musical de Psychic Force relacionado con su animación original en video. CD para seguidores de la franquicia y su música.', 'ACTIVO', 'CD', 'Anime', '/productos/catalogo/psychic-force-original-video-animation-mini-album-d7e8e382.jpg', 'Psychic Force - Original Video Animation Mini Album', NULL, 16990.00),
(119, 'SERIES_PELICULAS_ANIME', 'Anime de chicas mágicas que combina deseos, amistad y decisiones difíciles con una historia de fantasía y misterio.', 'ACTIVO', 'DVD', 'Anime', '/productos/catalogo/puella-magi-madoka-magica-8d6290e0.jpg', 'Puella Magi Madoka Magica', NULL, 24990.00),
(120, 'SERIES_PELICULAS_ANIME', 'Clásico de Frank Capra en el que George Bailey descubre cuánto ha influido su vida en las personas que lo rodean. Presentación Cinehome.', 'ACTIVO', 'DVD', 'Serie', '/productos/catalogo/que-bello-es-vivir-frank-capra-cinehome-6a43074d.jpg', 'Qué Bello Es Vivir - Frank Capra - Cinehome', NULL, 14990.00),
(121, 'SERIES_PELICULAS_ANIME', 'Colección de cinco películas de Rambo, con aventuras de acción protagonizadas por el veterano John Rambo.', 'ACTIVO', 'Blu-ray', 'Película', '/productos/catalogo/rambo-5-film-collection-9e87fb44.jpg', 'Rambo 5 Film Collection', NULL, 19990.00),
(122, 'SERIES_PELICULAS_ANIME', 'Taeko reflexiona sobre su infancia y las decisiones de su vida durante un viaje al campo. Drama animado de Studio Ghibli.', 'ACTIVO', 'DVD', 'Película', '/productos/catalogo/recuerdos-del-ayer-ghibli-a1e60b3f.jpg', 'Recuerdos del Ayer (Ghibli)', NULL, 14990.00),
(123, 'VIDEOJUEGOS', 'Aventura de mundo abierto ambientada en el ocaso del Viejo Oeste, protagonizada por Arthur Morgan y la banda de Van der Linde. Juego para PS4.', 'ACTIVO', 'Físico', 'Videojuego', '/productos/catalogo/red-dead-redemption-2-ce362b7a.jpg', 'Red Dead Redemption 2 (PS4)', 'PS4', 39990.00),
(124, 'SERIES_PELICULAS_ANIME', 'Historia de ciencia ficción ambientada en el universo Robotech, dedicada a la misión de los Centinelas. Presentación SD on BD.', 'ACTIVO', 'Blu-ray', 'Anime', '/productos/catalogo/robotech-ii-los-centinelas-sd-on-bd-fa80e073.jpg', 'Robotech II: Los Centinelas (SD on BD)', NULL, 24990.00),
(125, 'VIDEOJUEGOS', 'Aventura de búsqueda de objetos y puzles para PC en la que Samantha Swift investiga reliquias y misterios arqueológicos.', 'ACTIVO', 'Físico', 'Videojuego', '/productos/catalogo/samantha-swift-and-the-golden-touch-aa170cb3.jpg', 'Samantha Swift and the Golden Touch (PC)', 'PC', 39990.00),
(126, 'SERIES_PELICULAS_ANIME', 'Película de animación japonesa titulada Scarlet, en presentación Blu-ray. Ideal para quienes coleccionan estrenos y producciones de anime.', 'ACTIVO', 'Blu-ray', 'Anime', '/productos/catalogo/scarlet-blu-ray-4249d62f.jpg', 'Scarlet (Blu-ray)', NULL, 24990.00),
(127, 'SERIES_PELICULAS_ANIME', 'Película animada de Studio Ghibli que sigue a un joven apasionado por diseñar aviones y sus sueños en un Japón cambiante.', 'ACTIVO', 'DVD', 'Película', '/productos/catalogo/se-levanta-el-viento-ghibli-miyazaki-1ac37c3d.jpg', 'Se Levanta el Viento (Ghibli Miyazaki)', NULL, 14990.00),
(128, 'SERIES_PELICULAS_ANIME', 'Colección de televisión clásica de Paramount con 39 episodios. Una opción para quienes disfrutan revisitar programas históricos de la pantalla chica.', 'ACTIVO', 'DVD', 'Serie', '/productos/catalogo/serie-de-tv-paramount-39-episodios-clasicos-53c9ac73.jpg', 'Serie de TV Paramount 39 Episodios Clásicos', NULL, 21990.00),
(129, 'SERIES_PELICULAS_ANIME', 'Dos detectives investigan una serie de crímenes inspirados en los siete pecados capitales. Thriller oscuro de David Fincher.', 'ACTIVO', 'Blu-ray', 'Película', '/productos/catalogo/seven-los-siete-pecados-capitales-1812caf2.jpg', 'Seven: Los Siete Pecados Capitales', NULL, 19990.00),
(130, 'SERIES_PELICULAS_ANIME', 'Serie dramática histórica ambientada en Japón feudal, centrada en intrigas políticas, alianzas y choques culturales. Colección clásica en cuatro DVD.', 'ACTIVO', 'DVD', 'Serie', '/productos/catalogo/shogun-serie-completa-clasica-4-dvd-ee48f627.webp', 'Shogun Serie Completa Clásica 4 DVD', NULL, 21990.00),
(131, 'VIDEOJUEGOS', 'Experiencia de terror psicológico ambientada en el inquietante universo de Silent Hill. Juego para PS5.', 'ACTIVO', 'Físico', 'Videojuego', '/productos/catalogo/silent-hill-townfall-9aff0b40.jpg', 'Silent Hill: Townfall (PS5)', 'PS5', 39990.00),
(132, 'MERCHANDISING', 'Polera con diseño tipo collage dedicado al manga y anime de baloncesto Slam Dunk.', 'ACTIVO', 'Físico', 'Coleccionable', '/productos/catalogo/slam-dunk-collage-polera-22398a3f.jpg', 'Slam Dunk - Collage - Polera', NULL, 12990.00),
(133, 'SERIES_PELICULAS_ANIME', 'Sonic y sus amigos llegan al mundo humano y protagonizan carreras, batallas y aventuras frente a sus enemigos. Serie de anime en formato físico.', 'ACTIVO', 'DVD', 'Anime', '/productos/catalogo/sonic-x-c8e8caac.jpg', 'Sonic X', NULL, 24990.00),
(134, 'SERIES_PELICULAS_ANIME', 'Tercera temporada de Spartacus, centrada en la rebelión de esclavos contra la República romana. Box War of the Damned en cuatro DVD.', 'ACTIVO', 'DVD', 'Serie', '/productos/catalogo/spartacus-war-of-the-damned-temporada-3-4-dvd-799fcb7f.jpg', 'Spartacus War of the Damned - Temporada 3 (4 DVD)', NULL, 21990.00),
(135, 'VIDEOJUEGOS', 'Expansión clásica de estrategia en tiempo real para StarCraft, con nuevas campañas, unidades y enfrentamientos entre facciones. Edición física para PC.', 'ACTIVO', 'Físico', 'Videojuego', '/productos/catalogo/starcraft-brood-war-expansion-set-a856a289.jpg', 'StarCraft: Brood War Expansion Set (PC físico)', 'PC', 39990.00),
(136, 'SERIES_PELICULAS_ANIME', 'Thriller de ciencia ficción sobre un grupo de amigos cuyas investigaciones terminan alterando el tiempo. Colección con serie, película y OVA.', 'ACTIVO', 'DVD', 'Anime', '/productos/catalogo/steins-gate-serie-completa-pelicula-ova-2a4fe3db.jpg', 'Steins;Gate - Serie Completa + Película + OVA', NULL, 24990.00),
(137, 'SERIES_PELICULAS_ANIME', 'Superman regresa para enfrentar una nueva amenaza que pone en peligro la Tierra. Cuarta película de la saga clásica.', 'ACTIVO', 'Blu-ray', 'Película', '/productos/catalogo/superman-iv-14b4b58c.jpg', 'Superman IV', NULL, 19990.00),
(138, 'SERIES_PELICULAS_ANIME', 'Drama sobre un padre y un hijo que intentan sobrellevar las secuelas de una tragedia familiar en Nueva York.', 'ACTIVO', 'Blu-ray', 'Película', '/productos/catalogo/tan-fuerte-tan-cerca-acf53203.jpg', 'Tan Fuerte Tan Cerca', NULL, 19990.00),
(139, 'MERCHANDISING', 'Taza con diseño de Mr. Meeseeks, personaje de la serie animada Rick y Morty.', 'ACTIVO', 'Físico', 'Coleccionable', '/productos/catalogo/taza-mr-meeseeks-rick-y-morty-b0e5e8d5.jpg', 'Taza Mr. Meeseeks - Rick y Morty', NULL, 12990.00),
(140, 'SERIES_PELICULAS_ANIME', 'Un agente participa en una misión que utiliza una tecnología capaz de invertir el flujo temporal de ciertos objetos y acontecimientos. Thriller de Christopher Nolan.', 'ACTIVO', 'Blu-ray', 'Película', '/productos/catalogo/tenet-nolan-68005b7f.jpg', 'Tenet (Nolan)', NULL, 19990.00),
(141, 'SERIES_PELICULAS_ANIME', 'Drama televisivo sobre las vidas, amistades y relaciones de un grupo de mujeres en Los Ángeles. Colección completa en DVD.', 'ACTIVO', 'DVD', 'Serie', '/productos/catalogo/the-l-word-serie-completa-en-dvd-728df769.jpg', 'The L Word - Serie Completa en DVD', NULL, 21990.00),
(142, 'VIDEOJUEGOS', 'Juego de estrategia y construcción para PC, centrado en desarrollar asentamientos, gestionar recursos y expandir un imperio.', 'ACTIVO', 'Físico', 'Videojuego', '/productos/catalogo/the-settlers-construye-tu-imperio-8195feb5.jpg', 'The Settlers: Construye Tu Imperio (PC)', 'PC', 39990.00),
(143, 'SERIES_PELICULAS_ANIME', 'Truman empieza a sospechar que su aparentemente perfecta vida es parte de algo mucho más grande. Sátira dramática protagonizada por Jim Carrey.', 'ACTIVO', 'Blu-ray', 'Película', '/productos/catalogo/the-truman-show-e27e17bf.jpg', 'The Truman Show', NULL, 19990.00),
(144, 'SERIES_PELICULAS_ANIME', 'Un enorme tiburón blanco amenaza una localidad costera y obliga a un jefe de policía, un científico y un cazador a enfrentarlo.', 'ACTIVO', 'Blu-ray', 'Película', '/productos/catalogo/tiburon-bc633630.jpg', 'Tiburón', NULL, 19990.00),
(145, 'SERIES_PELICULAS_ANIME', 'Melodrama de Pedro Almodóvar sobre la maternidad, el duelo y los vínculos inesperados entre varias mujeres.', 'ACTIVO', 'DVD', 'Película', '/productos/catalogo/todo-sobre-mi-madre-pedro-almodovar-13067a6b.jpg', 'Todo Sobre Mi Madre (Pedro Almodóvar)', NULL, 14990.00),
(146, 'VIDEOJUEGOS', 'Aventura de objetos ocultos y misterio para PC, presentada en edición física DVD con contenido adicional indicado en el título.', 'ACTIVO', 'Físico', 'Videojuego', '/productos/catalogo/twisted-lands-origin-4-pack-bonus-edition-ee21368a.jpg', 'Twisted Lands Origin 4 Pack Bonus Edition (PC DVD)', 'PC', 39990.00),
(147, 'SERIES_PELICULAS_ANIME', 'Serie animada de ciencia ficción y aventuras espaciales inspirada en la Odisea. Presentación Blu-ray custom indicada en el título.', 'ACTIVO', 'Blu-ray', 'Anime', '/productos/catalogo/ulysse-31-blu-ray-custom-5ab1a04c.jpg', 'Ulysse 31 (Blu-ray Custom)', NULL, 24990.00),
(148, 'SERIES_PELICULAS_ANIME', 'Momo encuentra cartas misteriosas y conoce criaturas sobrenaturales tras mudarse a una isla. Película de animación japonesa.', 'ACTIVO', 'Blu-ray', 'Película', '/productos/catalogo/una-carta-para-momo-289cf847.jpg', 'Una Carta para Momo', NULL, 19990.00),
(149, 'SERIES_PELICULAS_ANIME', 'Drama chileno sobre Marina, una mujer que lucha por vivir su duelo con dignidad frente a los prejuicios y las dificultades.', 'ACTIVO', 'DVD', 'Película', '/productos/catalogo/una-mujer-fantastica-pelicula-chilena-629162bf.jpg', 'Una Mujer Fantástica (película chilena)', NULL, 14990.00),
(150, 'SERIES_PELICULAS_ANIME', 'Carl Fredricksen emprende un viaje extraordinario llevando su casa por los aires, acompañado por el joven Russell. Aventura animada de Pixar.', 'ACTIVO', 'DVD', 'Película', '/productos/catalogo/up-una-aventura-de-altura-disney-pixar-958e9f3c.jpg', 'Up: Una Aventura de Altura (Disney Pixar)', NULL, 14990.00),
(151, 'SERIES_PELICULAS_ANIME', 'Colección de la saga de fantasía oscura Urotsukidoji. Edición SD on BD indicada en el título; contenido dirigido a público adulto.', 'ACTIVO', 'Blu-ray', 'Anime', '/productos/catalogo/urotsukidoji-the-perfect-collection-sd-on-bd-e88a0d86.jpg', 'Urotsukidoji: The Perfect Collection (SD on BD)', NULL, 24990.00),
(152, 'SERIES_PELICULAS_ANIME', 'Anime de terror sobrenatural protagonizado por una princesa vampiro encargada de enfrentar seres oscuros. Colección Blu-ray Box.', 'ACTIVO', 'Blu-ray', 'Anime', '/productos/catalogo/vampire-princess-miyu-blu-ray-box-a35719a9.jpg', 'Vampire Princess Miyu - Blu-ray Box', NULL, 24990.00),
(153, 'SERIES_PELICULAS_ANIME', 'Propuesta de ciencia ficción clásica que reúne los títulos Viaje al Séptimo Planeta y Guerra de Mundos. Presentación física en DVD.', 'ACTIVO', 'DVD', 'Serie', '/productos/catalogo/viaje-al-septimo-planeta-guerra-de-mundos-dvd-0cd115d8.jpg', 'Viaje al Séptimo Planeta / Guerra de Mundos - DVD', NULL, 14990.00),
(154, 'SERIES_PELICULAS_ANIME', 'Comedia dramática de Pedro Almodóvar sobre una ama de casa madrileña y las tensiones cotidianas de su entorno familiar.', 'ACTIVO', 'DVD', 'Película', '/productos/catalogo/que-he-hecho-yo-para-merecer-esto-03be8603.jpg', '¿Qué He Hecho Yo Para Merecer Esto? (Pedro Almodóvar)', NULL, 14990.00),
(155, 'SERIES_PELICULAS_ANIME', 'Un detective investiga un caso que reúne personajes animados y personas reales en el Hollywood de los años cuarenta.', 'ACTIVO', 'Blu-ray', 'Película', '/productos/catalogo/quien-engano-a-roger-rabbit-7f2b624c.jpg', '¿Quién Engañó a Roger Rabbit?', NULL, 19990.00),
(165, 'VIDEOJUEGOS', 'Aventura narrativa de ciencia ficción sobre identidad, recuerdos y resistencia. Preventa para PS5; publicación en catálogo, sin compra habilitada.', 'ACTIVO', 'Físico', 'Videojuego', '/productos/catalogo/1000xresist-ps5-preventa-f93ad8d9.png', '1000xRESIST (PS5) - Preventa', 'PS5', 39990.00),
(166, 'VIDEOJUEGOS', 'Colección de los dos juegos Citizen Sleeper, aventuras narrativas de ciencia ficción sobre supervivencia y decisiones. Preventa para PS5, sin compra habilitada.', 'ACTIVO', 'Físico', 'Videojuego', '/productos/catalogo/citizen-sleeper-1-2-helion-collection-ps5-preventa-9b0a2f3f.jpg', 'Citizen Sleeper 1+2 Helion Collection (PS5) - Preventa', 'PS5', 39990.00),
(167, 'VIDEOJUEGOS', 'Nuevo título de misterio de la franquicia Danganronpa para PS5. Preventa indicada en el catálogo; compra no habilitada.', 'ACTIVO', 'Físico', 'Videojuego', '/productos/catalogo/danganronpa-2x2-ps5-preventa-570b907f.jpg', 'Danganronpa 2x2 (PS5) - Preventa', 'PS5', 39990.00),
(168, 'VIDEOJUEGOS', 'RPG de fantasía y aventura anunciado para PS5. Producto en preventa, visible en catálogo sin compra habilitada.', 'ACTIVO', 'Físico', 'Videojuego', '/productos/catalogo/edge-of-memories-ps5-preventa-81cfd955.png', 'Edge of Memories (PS5) - Preventa', 'PS5', 39990.00),
(169, 'VIDEOJUEGOS', 'Nueva entrega de la serie Grand Theft Auto, ambientada en un mundo abierto contemporáneo. Versión Xbox Series X; disponibilidad por confirmar y compra no habilitada.', 'ACTIVO', 'Físico', 'Videojuego', '/productos/catalogo/grand-theft-auto-vi-xbox-series-x-b1851c73.webp', 'Grand Theft Auto VI (Xbox Series X)', 'Xbox Series X', 39990.00),
(170, 'MERCHANDISING', 'Colección de cinco variantes de anillos inspirados en Orb: On the Movements of the Earth. Disponibilidad por confirmar; compra no habilitada.', 'ACTIVO', 'Físico', 'Coleccionable', '/productos/catalogo/orb-on-the-movements-of-the-earth-silver-rings-5-types-f3821c1a.jpg', 'Orb: On the Movements of the Earth - Silver Rings (5 Types)', NULL, 12990.00),
(171, 'VIDEOJUEGOS', 'Título para PS5 anunciado como Resonance: A Plague Tale Legacy. Disponibilidad por confirmar; producto de catálogo sin compra habilitada.', 'ACTIVO', 'Físico', 'Videojuego', '/productos/catalogo/resonance-a-plague-tale-legacy-ps5-39fbfe3c.jpg', 'Resonance: A Plague Tale Legacy (PS5)', 'PS5', 39990.00);

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `promocion`
--

CREATE TABLE `promocion` (
  `id` bigint(20) NOT NULL,
  `categoria` enum('ACCESORIOS','MERCHANDISING','MUSICA','SERIES_PELICULAS_ANIME','VIDEOJUEGOS') DEFAULT NULL,
  `desactivada` bit(1) NOT NULL,
  `fecha_desactivacion` datetime(6) DEFAULT NULL,
  `fecha_fin` date NOT NULL,
  `fecha_inicio` date NOT NULL,
  `monto_minimo_compra` decimal(12,2) DEFAULT NULL,
  `nombre` varchar(120) NOT NULL,
  `tipo_descuento` enum('MONTO','PORCENTAJE') NOT NULL,
  `valor` decimal(12,2) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `promocion_producto`
--

CREATE TABLE `promocion_producto` (
  `promocion_id` bigint(20) NOT NULL,
  `producto_id` bigint(20) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Índices para tablas volcadas
--

--
-- Indices de la tabla `favorito`
--
ALTER TABLE `favorito`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `uk_favorito_user_producto` (`user_id`,`producto_id`),
  ADD KEY `FKam2d6jb3e39532amsnb3m65wk` (`producto_id`);

--
-- Indices de la tabla `inventario`
--
ALTER TABLE `inventario`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `uk_inventario_product` (`product_id`);

--
-- Indices de la tabla `movimiento_stock`
--
ALTER TABLE `movimiento_stock`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `uk_mov_operacion_inventario` (`operacion_id`,`inventario_id`),
  ADD KEY `idx_mov_fecha` (`fecha_hora`),
  ADD KEY `FKje6wfvt36fv0aq2h1fux3wl4b` (`inventario_id`);

--
-- Indices de la tabla `producto`
--
ALTER TABLE `producto`
  ADD PRIMARY KEY (`id`),
  ADD KEY `idx_producto_categoria` (`categoria`),
  ADD KEY `idx_producto_estado` (`estado`);

--
-- Indices de la tabla `promocion`
--
ALTER TABLE `promocion`
  ADD PRIMARY KEY (`id`);

--
-- Indices de la tabla `promocion_producto`
--
ALTER TABLE `promocion_producto`
  ADD PRIMARY KEY (`promocion_id`,`producto_id`),
  ADD KEY `FKoyx8gqcjyab8hctqimjenmj3g` (`producto_id`);

--
-- AUTO_INCREMENT de las tablas volcadas
--

--
-- AUTO_INCREMENT de la tabla `favorito`
--
ALTER TABLE `favorito`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT de la tabla `inventario`
--
ALTER TABLE `inventario`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=172;

--
-- AUTO_INCREMENT de la tabla `movimiento_stock`
--
ALTER TABLE `movimiento_stock`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=10;

--
-- AUTO_INCREMENT de la tabla `producto`
--
ALTER TABLE `producto`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=172;

--
-- AUTO_INCREMENT de la tabla `promocion`
--
ALTER TABLE `promocion`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT;

--
-- Restricciones para tablas volcadas
--

--
-- Filtros para la tabla `favorito`
--
ALTER TABLE `favorito`
  ADD CONSTRAINT `FKam2d6jb3e39532amsnb3m65wk` FOREIGN KEY (`producto_id`) REFERENCES `producto` (`id`);

--
-- Filtros para la tabla `inventario`
--
ALTER TABLE `inventario`
  ADD CONSTRAINT `FKs13h14qd6d4fr8o4kuv5pf4ia` FOREIGN KEY (`product_id`) REFERENCES `producto` (`id`);

--
-- Filtros para la tabla `movimiento_stock`
--
ALTER TABLE `movimiento_stock`
  ADD CONSTRAINT `FKje6wfvt36fv0aq2h1fux3wl4b` FOREIGN KEY (`inventario_id`) REFERENCES `inventario` (`id`);

--
-- Filtros para la tabla `promocion_producto`
--
ALTER TABLE `promocion_producto`
  ADD CONSTRAINT `FKo8i7f4ehqxnyfoxb83p74djkl` FOREIGN KEY (`promocion_id`) REFERENCES `promocion` (`id`),
  ADD CONSTRAINT `FKoyx8gqcjyab8hctqimjenmj3g` FOREIGN KEY (`producto_id`) REFERENCES `producto` (`id`);
COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
