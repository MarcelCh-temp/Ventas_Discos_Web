-- Ñackson Store — catálogo de prueba. PRECIOS y STOCK FICTICIOS, CONFIRMAR antes de uso real.

-- Ejecutar solo en db_catalog, después de hacer copia de seguridad.

-- NO elimina ni reemplaza productos previos. Omite coincidencias exactas por nombre + categoría.

-- La categoría SERIES_PELICULAS_ANIME es una categoría técnica COMPARTIDA: NO eliminar.

-- Las preventas se excluyen de esta carga para evitar vender stock ficticio.

-- imagen_url se deja NULL: aún no hay fotos verificadas para estos productos.

USE `db_catalog`;

SET NAMES utf8mb4;

CREATE TEMPORARY TABLE tmp_nackson_lote (nombre varchar(150) NOT NULL, categoria varchar(60) NOT NULL, descripcion varchar(2000), formato varchar(60) NOT NULL, genero varchar(60) NOT NULL, plataforma varchar(60), precio_base decimal(12,2) NOT NULL, stock_actual int NOT NULL, PRIMARY KEY(nombre,categoria)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO tmp_nackson_lote(nombre,categoria,descripcion,formato,genero,plataforma,precio_base,stock_actual) VALUES

('Shogun Serie Completa Clásica 4 DVD','SERIES_PELICULAS_ANIME','Catálogo de demostración Ñackson Store: Shogun Serie Completa Clásica 4 DVD. Reubicado desde Series; verificar edición','DVD','Serie TV',NULL,21990,10),
('Serie de TV Paramount 39 Episodios Clásicos','SERIES_PELICULAS_ANIME','Catálogo de demostración Ñackson Store: Serie de TV Paramount 39 Episodios Clásicos. Reubicado desde Series; verificar edición','DVD','Serie TV',NULL,21990,10),
('Ben 10 Classic - Colección Completa Warner Bros (DVD)','SERIES_PELICULAS_ANIME','Catálogo de demostración Ñackson Store: Ben 10 Classic - Colección Completa Warner Bros (DVD). Reubicado desde Series; verificar formato','DVD','Animación',NULL,18990,10),
('Kim Possible - La Serie Animada Clásica','SERIES_PELICULAS_ANIME','Catálogo de demostración Ñackson Store: Kim Possible - La Serie Animada Clásica. Reubicado desde Series; verificar formato','DVD','Animación',NULL,18990,10),
('Los Archivos Místicos de Dantalian - La Serie Completa','SERIES_PELICULAS_ANIME','Catálogo de demostración Ñackson Store: Los Archivos Místicos de Dantalian - La Serie Completa. Reubicado desde Series; verificar formato','DVD','Animación',NULL,18990,10),
('Iron Man - Serie Animada Completa de 1994 (3 DVD, usado)','SERIES_PELICULAS_ANIME','Catálogo de demostración Ñackson Store: Iron Man - Serie Animada Completa de 1994 (3 DVD, usado). Reubicado desde Series; verificar edición','DVD','Serie TV',NULL,21990,10),
('Lo Que el Viento Se Llevó - DVD Original','SERIES_PELICULAS_ANIME','Catálogo de demostración Ñackson Store: Lo Que el Viento Se Llevó - DVD Original. Reubicado desde Series; revisar título/edición exactos','DVD','Película',NULL,14990,10),
('Viaje al Séptimo Planeta / Guerra de Mundos - DVD','SERIES_PELICULAS_ANIME','Catálogo de demostración Ñackson Store: Viaje al Séptimo Planeta / Guerra de Mundos - DVD. Reubicado desde Series; revisar título/edición exactos','DVD','Película',NULL,14990,10),
('Qué Bello Es Vivir - Frank Capra - Cinehome','SERIES_PELICULAS_ANIME','Catálogo de demostración Ñackson Store: Qué Bello Es Vivir - Frank Capra - Cinehome. Reubicado desde Series; revisar título/edición exactos','DVD','Película',NULL,14990,10),
('La Dimensión Desconocida - Serie de TV Paramount Completa','SERIES_PELICULAS_ANIME','Catálogo de demostración Ñackson Store: La Dimensión Desconocida - Serie de TV Paramount Completa. Reubicado desde Series; verificar edición','DVD','Serie TV',NULL,21990,10),
('Spartacus War of the Damned - Temporada 3 (4 DVD)','SERIES_PELICULAS_ANIME','Catálogo de demostración Ñackson Store: Spartacus War of the Damned - Temporada 3 (4 DVD). Reubicado desde Series; verificar edición','DVD','Serie TV',NULL,21990,10),
('Área 12 - Serie de TV Español Latino (6 DVD)','SERIES_PELICULAS_ANIME','Catálogo de demostración Ñackson Store: Área 12 - Serie de TV Español Latino (6 DVD). Reubicado desde Series; verificar edición','DVD','Serie TV',NULL,21990,10),
('Futurama - Serie Original en DVD','SERIES_PELICULAS_ANIME','Catálogo de demostración Ñackson Store: Futurama - Serie Original en DVD. Reubicado desde Series; verificar edición','DVD','Serie TV',NULL,21990,10),
('The L Word - Serie Completa en DVD','SERIES_PELICULAS_ANIME','Catálogo de demostración Ñackson Store: The L Word - Serie Completa en DVD. Reubicado desde Series; verificar edición','DVD','Serie TV',NULL,21990,10),
('Gossip Girl - The Complete Series (DVD)','SERIES_PELICULAS_ANIME','Catálogo de demostración Ñackson Store: Gossip Girl - The Complete Series (DVD). Reubicado desde Series; verificar edición','DVD','Serie TV',NULL,21990,10),
('Avengers: Infinity War (Marvel)','SERIES_PELICULAS_ANIME','Catálogo de demostración Ñackson Store: Avengers: Infinity War (Marvel). Formato DVD provisional: confirmar edición antes de venta','DVD','Película',NULL,14990,12),
('El Gran Pez (Tim Burton)','SERIES_PELICULAS_ANIME','Catálogo de demostración Ñackson Store: El Gran Pez (Tim Burton). Formato DVD provisional: confirmar edición antes de venta','DVD','Película',NULL,14990,12),
('Operación Red Sparrow','SERIES_PELICULAS_ANIME','Catálogo de demostración Ñackson Store: Operación Red Sparrow. Formato DVD provisional: confirmar edición antes de venta','DVD','Película',NULL,14990,12),
('Una Mujer Fantástica (película chilena)','SERIES_PELICULAS_ANIME','Catálogo de demostración Ñackson Store: Una Mujer Fantástica (película chilena). Formato DVD provisional: confirmar edición antes de venta','DVD','Película',NULL,14990,12),
('Bichos: Una Aventura en Miniatura (Disney)','SERIES_PELICULAS_ANIME','Catálogo de demostración Ñackson Store: Bichos: Una Aventura en Miniatura (Disney). Formato DVD provisional: confirmar edición antes de venta','DVD','Película',NULL,14990,12),
('Horton y el Mundo de los Quién','SERIES_PELICULAS_ANIME','Catálogo de demostración Ñackson Store: Horton y el Mundo de los Quién. Formato DVD provisional: confirmar edición antes de venta','DVD','Película',NULL,14990,12),
('Todo Sobre Mi Madre (Pedro Almodóvar)','SERIES_PELICULAS_ANIME','Catálogo de demostración Ñackson Store: Todo Sobre Mi Madre (Pedro Almodóvar). Formato DVD provisional: confirmar edición antes de venta','DVD','Película',NULL,14990,12),
('Se Levanta el Viento (Ghibli Miyazaki)','SERIES_PELICULAS_ANIME','Catálogo de demostración Ñackson Store: Se Levanta el Viento (Ghibli Miyazaki). Formato DVD provisional: confirmar edición antes de venta','DVD','Película',NULL,14990,12),
('Kill Bill Volumen 2 (Tarantino)','SERIES_PELICULAS_ANIME','Catálogo de demostración Ñackson Store: Kill Bill Volumen 2 (Tarantino). Formato DVD provisional: confirmar edición antes de venta','DVD','Película',NULL,14990,12),
('¿Qué He Hecho Yo Para Merecer Esto? (Pedro Almodóvar)','SERIES_PELICULAS_ANIME','Catálogo de demostración Ñackson Store: ¿Qué He Hecho Yo Para Merecer Esto? (Pedro Almodóvar). Formato DVD provisional: confirmar edición antes de venta','DVD','Película',NULL,14990,12),
('Up: Una Aventura de Altura (Disney Pixar)','SERIES_PELICULAS_ANIME','Catálogo de demostración Ñackson Store: Up: Una Aventura de Altura (Disney Pixar). Formato DVD provisional: confirmar edición antes de venta','DVD','Película',NULL,14990,12),
('El Recuerdo de Marnie (Ghibli Miyazaki)','SERIES_PELICULAS_ANIME','Catálogo de demostración Ñackson Store: El Recuerdo de Marnie (Ghibli Miyazaki). Formato DVD provisional: confirmar edición antes de venta','DVD','Película',NULL,14990,12),
('El Castillo en el Cielo (Ghibli Miyazaki)','SERIES_PELICULAS_ANIME','Catálogo de demostración Ñackson Store: El Castillo en el Cielo (Ghibli Miyazaki). Formato DVD provisional: confirmar edición antes de venta','DVD','Película',NULL,14990,12),
('Recuerdos del Ayer (Ghibli)','SERIES_PELICULAS_ANIME','Catálogo de demostración Ñackson Store: Recuerdos del Ayer (Ghibli). Formato DVD provisional: confirmar edición antes de venta','DVD','Película',NULL,14990,12),
('África Mía','SERIES_PELICULAS_ANIME','Catálogo de demostración Ñackson Store: África Mía. Formato DVD provisional: confirmar edición antes de venta','DVD','Película',NULL,14990,12),
('101 Dálmatas (Disney)','SERIES_PELICULAS_ANIME','Catálogo de demostración Ñackson Store: 101 Dálmatas (Disney). Formato DVD provisional: confirmar edición antes de venta','DVD','Película',NULL,14990,12),
('Anastasia','SERIES_PELICULAS_ANIME','Catálogo de demostración Ñackson Store: Anastasia. Formato DVD provisional: confirmar edición antes de venta','DVD','Película',NULL,14990,12),
('La Bella Durmiente (Disney)','SERIES_PELICULAS_ANIME','Catálogo de demostración Ñackson Store: La Bella Durmiente (Disney). Formato DVD provisional: confirmar edición antes de venta','DVD','Película',NULL,14990,12),
('Breaking Bad - Temporada 3','SERIES_PELICULAS_ANIME','Catálogo de demostración Ñackson Store: Breaking Bad - Temporada 3. Reubicado: no es película; formato a confirmar','DVD','Serie TV',NULL,18990,10),
('Mi Bella Dama (My Fair Lady)','SERIES_PELICULAS_ANIME','Catálogo de demostración Ñackson Store: Mi Bella Dama (My Fair Lady). Formato DVD provisional: confirmar edición antes de venta','DVD','Película',NULL,14990,12),
('Outlanders (Blu-ray)','SERIES_PELICULAS_ANIME','Catálogo de demostración Ñackson Store: Outlanders (Blu-ray). Verificar edición y formato','Blu-ray','Anime',NULL,24990,10),
('Ulysse 31 (Blu-ray Custom)','SERIES_PELICULAS_ANIME','Catálogo de demostración Ñackson Store: Ulysse 31 (Blu-ray Custom). Edición custom/SD on BD: revisar derechos y disponibilidad','Blu-ray','Anime',NULL,24990,10),
('Hajime no Ippo: New Challenger (Blu-ray Custom)','SERIES_PELICULAS_ANIME','Catálogo de demostración Ñackson Store: Hajime no Ippo: New Challenger (Blu-ray Custom). Edición custom/SD on BD: revisar derechos y disponibilidad','Blu-ray','Anime',NULL,24990,10),
('Vampire Princess Miyu - Blu-ray Box','SERIES_PELICULAS_ANIME','Catálogo de demostración Ñackson Store: Vampire Princess Miyu - Blu-ray Box. Verificar edición y formato','Blu-ray','Anime',NULL,24990,10),
('Arrow Emblem Grand Prix no Taka (Blu-ray Box Custom)','SERIES_PELICULAS_ANIME','Catálogo de demostración Ñackson Store: Arrow Emblem Grand Prix no Taka (Blu-ray Box Custom). Edición custom/SD on BD: revisar derechos y disponibilidad','Blu-ray','Anime',NULL,24990,10),
('Urotsukidoji: The Perfect Collection (SD on BD)','SERIES_PELICULAS_ANIME','Catálogo de demostración Ñackson Store: Urotsukidoji: The Perfect Collection (SD on BD). Edición custom/SD on BD: revisar derechos y disponibilidad','Blu-ray','Anime',NULL,24990,10),
('Jet Marte (SD on BD Blu-ray)','SERIES_PELICULAS_ANIME','Catálogo de demostración Ñackson Store: Jet Marte (SD on BD Blu-ray). Edición custom/SD on BD: revisar derechos y disponibilidad','Blu-ray','Anime',NULL,24990,10),
('Patlabor EZY File-1 (Blu-ray)','SERIES_PELICULAS_ANIME','Catálogo de demostración Ñackson Store: Patlabor EZY File-1 (Blu-ray). Verificar edición y formato','Blu-ray','Anime',NULL,24990,10),
('Robotech II: Los Centinelas (SD on BD)','SERIES_PELICULAS_ANIME','Catálogo de demostración Ñackson Store: Robotech II: Los Centinelas (SD on BD). Edición custom/SD on BD: revisar derechos y disponibilidad','Blu-ray','Anime',NULL,24990,10),
('Scarlet (Blu-ray)','SERIES_PELICULAS_ANIME','Catálogo de demostración Ñackson Store: Scarlet (Blu-ray). Verificar edición y formato','Blu-ray','Anime',NULL,24990,10),
('Infinite Stratos - Original Soundtrack','MUSICA','Catálogo de demostración Ñackson Store: Infinite Stratos - Original Soundtrack. Disco de música, no video; formato CD por confirmar','CD','Soundtrack de anime',NULL,16990,10),
('Psychic Force - Original Video Animation Mini Album','MUSICA','Catálogo de demostración Ñackson Store: Psychic Force - Original Video Animation Mini Album. Disco de música, no video; formato CD por confirmar','CD','Soundtrack de anime',NULL,16990,10),
('Fullmetal Alchemist - Temporadas 1 y 2 + Películas + OVA (DVD)','SERIES_PELICULAS_ANIME','Catálogo de demostración Ñackson Store: Fullmetal Alchemist - Temporadas 1 y 2 + Películas + OVA (DVD). Verificar edición y formato','DVD','Anime',NULL,24990,10),
('Bleach - Serie TV Vol. 1-366 + Películas + Especiales (DVD)','SERIES_PELICULAS_ANIME','Catálogo de demostración Ñackson Store: Bleach - Serie TV Vol. 1-366 + Películas + Especiales (DVD). Verificar edición y formato','DVD','Anime',NULL,24990,10),
('Sonic X','SERIES_PELICULAS_ANIME','Catálogo de demostración Ñackson Store: Sonic X. Verificar edición y formato','DVD','Anime',NULL,24990,10),
('Steins;Gate - Serie Completa + Película + OVA','SERIES_PELICULAS_ANIME','Catálogo de demostración Ñackson Store: Steins;Gate - Serie Completa + Película + OVA. Verificar edición y formato','DVD','Anime',NULL,24990,10),
('Puella Magi Madoka Magica','SERIES_PELICULAS_ANIME','Catálogo de demostración Ñackson Store: Puella Magi Madoka Magica. Verificar edición y formato','DVD','Anime',NULL,24990,10),
('Memories','SERIES_PELICULAS_ANIME','Catálogo de demostración Ñackson Store: Memories. Verificar edición y formato','DVD','Anime',NULL,24990,10),
('Ghost in the Shell','SERIES_PELICULAS_ANIME','Catálogo de demostración Ñackson Store: Ghost in the Shell. Verificar edición y formato','DVD','Anime',NULL,24990,10),
('El Club de la Pelea','SERIES_PELICULAS_ANIME','Catálogo de demostración Ñackson Store: El Club de la Pelea. Verificar edición y portada','Blu-ray','Película',NULL,19990,10),
('Hombres de Negro 1 y 2','SERIES_PELICULAS_ANIME','Catálogo de demostración Ñackson Store: Hombres de Negro 1 y 2. Verificar edición y portada','Blu-ray','Película',NULL,19990,10),
('Ant-Man and The Wasp','SERIES_PELICULAS_ANIME','Catálogo de demostración Ñackson Store: Ant-Man and The Wasp. Verificar edición y portada','Blu-ray','Película',NULL,19990,10),
('Asesinato en el Expreso de Oriente','SERIES_PELICULAS_ANIME','Catálogo de demostración Ñackson Store: Asesinato en el Expreso de Oriente. Verificar edición y portada','Blu-ray','Película',NULL,19990,10),
('Tan Fuerte Tan Cerca','SERIES_PELICULAS_ANIME','Catálogo de demostración Ñackson Store: Tan Fuerte Tan Cerca. Verificar edición y portada','Blu-ray','Película',NULL,19990,10),
('La Cosa (The Thing)','SERIES_PELICULAS_ANIME','Catálogo de demostración Ñackson Store: La Cosa (The Thing). Verificar edición y portada','Blu-ray','Película',NULL,19990,10),
('The Truman Show','SERIES_PELICULAS_ANIME','Catálogo de demostración Ñackson Store: The Truman Show. Verificar edición y portada','Blu-ray','Película',NULL,19990,10),
('Christopher Robin: Un Reencuentro Inolvidable','SERIES_PELICULAS_ANIME','Catálogo de demostración Ñackson Store: Christopher Robin: Un Reencuentro Inolvidable. Verificar edición y portada','Blu-ray','Película',NULL,19990,10),
('La Noche Más Oscura (Zero Dark Thirty)','SERIES_PELICULAS_ANIME','Catálogo de demostración Ñackson Store: La Noche Más Oscura (Zero Dark Thirty). Verificar edición y portada','Blu-ray','Película',NULL,19990,10),
('¿Quién Engañó a Roger Rabbit?','SERIES_PELICULAS_ANIME','Catálogo de demostración Ñackson Store: ¿Quién Engañó a Roger Rabbit?. Verificar edición y portada','Blu-ray','Película',NULL,19990,10),
('Dúo Día de la Independencia / Contraataque','SERIES_PELICULAS_ANIME','Catálogo de demostración Ñackson Store: Dúo Día de la Independencia / Contraataque. Verificar edición y portada','Blu-ray','Película',NULL,19990,10),
('Constantine','SERIES_PELICULAS_ANIME','Catálogo de demostración Ñackson Store: Constantine. Verificar edición y portada','Blu-ray','Película',NULL,19990,10),
('Linterna Verde','SERIES_PELICULAS_ANIME','Catálogo de demostración Ñackson Store: Linterna Verde. Verificar edición y portada','Blu-ray','Película',NULL,19990,10),
('Superman IV','SERIES_PELICULAS_ANIME','Catálogo de demostración Ñackson Store: Superman IV. Verificar edición y portada','Blu-ray','Película',NULL,19990,10),
('Assassin’s Creed','SERIES_PELICULAS_ANIME','Catálogo de demostración Ñackson Store: Assassin’s Creed. Verificar edición y portada','Blu-ray','Película',NULL,19990,10),
('Aquaman (Edición Steelbook)','SERIES_PELICULAS_ANIME','Catálogo de demostración Ñackson Store: Aquaman (Edición Steelbook). Verificar edición y portada','Blu-ray','Película',NULL,19990,10),
('Seven: Los Siete Pecados Capitales','SERIES_PELICULAS_ANIME','Catálogo de demostración Ñackson Store: Seven: Los Siete Pecados Capitales. Verificar edición y portada','Blu-ray','Película',NULL,19990,10),
('Tiburón','SERIES_PELICULAS_ANIME','Catálogo de demostración Ñackson Store: Tiburón. Verificar edición y portada','Blu-ray','Película',NULL,19990,10),
('Rambo 5 Film Collection','SERIES_PELICULAS_ANIME','Catálogo de demostración Ñackson Store: Rambo 5 Film Collection. Verificar edición y portada','Blu-ray','Película',NULL,19990,10),
('Corazón de Caballero','SERIES_PELICULAS_ANIME','Catálogo de demostración Ñackson Store: Corazón de Caballero. Verificar edición y portada','Blu-ray','Película',NULL,19990,10),
('John Wick Capítulos 1-4','SERIES_PELICULAS_ANIME','Catálogo de demostración Ñackson Store: John Wick Capítulos 1-4. Verificar edición y portada','Blu-ray','Película',NULL,19990,10),
('Dúo Alien vs Depredador 1 y 2','SERIES_PELICULAS_ANIME','Catálogo de demostración Ñackson Store: Dúo Alien vs Depredador 1 y 2. Verificar edición y portada','Blu-ray','Película',NULL,19990,10),
('Tenet (Nolan)','SERIES_PELICULAS_ANIME','Catálogo de demostración Ñackson Store: Tenet (Nolan). Verificar edición y portada','Blu-ray','Película',NULL,19990,10),
('Hombres de Negro','SERIES_PELICULAS_ANIME','Catálogo de demostración Ñackson Store: Hombres de Negro. Posible duplicado de Hombres de Negro','Blu-ray','Película',NULL,19990,10),
('Una Carta para Momo','SERIES_PELICULAS_ANIME','Catálogo de demostración Ñackson Store: Una Carta para Momo. Verificar edición y portada','Blu-ray','Película',NULL,19990,10),
('The Settlers: Construye Tu Imperio (PC)','VIDEOJUEGOS','Catálogo de demostración Ñackson Store: The Settlers: Construye Tu Imperio (PC). Verificar existencia física/fecha de lanzamiento','Físico','Videojuego','PC',39990,10),
('Twisted Lands Origin 4 Pack Bonus Edition (PC DVD)','VIDEOJUEGOS','Catálogo de demostración Ñackson Store: Twisted Lands Origin 4 Pack Bonus Edition (PC DVD). Verificar existencia física/fecha de lanzamiento','Físico','Videojuego','PC',39990,10),
('StarCraft: Brood War Expansion Set (PC físico)','VIDEOJUEGOS','Catálogo de demostración Ñackson Store: StarCraft: Brood War Expansion Set (PC físico). Verificar existencia física/fecha de lanzamiento','Físico','Videojuego','PC',39990,10),
('Samantha Swift and the Golden Touch (PC)','VIDEOJUEGOS','Catálogo de demostración Ñackson Store: Samantha Swift and the Golden Touch (PC). Verificar existencia física/fecha de lanzamiento','Físico','Videojuego','PC',39990,10),
('Disciples III (PC físico)','VIDEOJUEGOS','Catálogo de demostración Ñackson Store: Disciples III (PC físico). Verificar existencia física/fecha de lanzamiento','Físico','Videojuego','PC',39990,10),
('Red Dead Redemption 2 (PS4)','VIDEOJUEGOS','Catálogo de demostración Ñackson Store: Red Dead Redemption 2 (PS4). Verificar existencia física/fecha de lanzamiento','Físico','Videojuego','PS4',39990,10),
('Elden Ring Tarnished Edition (Switch 2)','VIDEOJUEGOS','Catálogo de demostración Ñackson Store: Elden Ring Tarnished Edition (Switch 2). Verificar existencia física/fecha de lanzamiento','Físico','Videojuego','Switch 2',39990,10),
('Pragmata (PS5)','VIDEOJUEGOS','Catálogo de demostración Ñackson Store: Pragmata (PS5). Verificar existencia física/fecha de lanzamiento','Físico','Videojuego','PS5',39990,10),
('Silent Hill: Townfall (PS5)','VIDEOJUEGOS','Catálogo de demostración Ñackson Store: Silent Hill: Townfall (PS5). Verificar existencia física/fecha de lanzamiento','Físico','Videojuego','PS5',39990,10),
('Coffee Talk Tokyo (PS5)','VIDEOJUEGOS','Catálogo de demostración Ñackson Store: Coffee Talk Tokyo (PS5). Verificar existencia física/fecha de lanzamiento','Físico','Videojuego','PS5',39990,10),
('Corpse Party 2: Darkness Distortion (PS5)','VIDEOJUEGOS','Catálogo de demostración Ñackson Store: Corpse Party 2: Darkness Distortion (PS5). Verificar existencia física/fecha de lanzamiento','Físico','Videojuego','PS5',39990,10),
('Crisis Core: Final Fantasy VII Reunion (PS5)','VIDEOJUEGOS','Catálogo de demostración Ñackson Store: Crisis Core: Final Fantasy VII Reunion (PS5). Verificar existencia física/fecha de lanzamiento','Físico','Videojuego','PS5',39990,10),
('Culdcept The First Saturn Tribute (PS5)','VIDEOJUEGOS','Catálogo de demostración Ñackson Store: Culdcept The First Saturn Tribute (PS5). Verificar existencia física/fecha de lanzamiento','Físico','Videojuego','PS5',39990,10),
('Cyberpunk 2077: Ultimate Edition (PS5)','VIDEOJUEGOS','Catálogo de demostración Ñackson Store: Cyberpunk 2077: Ultimate Edition (PS5). Verificar existencia física/fecha de lanzamiento','Físico','Videojuego','PS5',39990,10),
('Dead Cells: Return to Castlevania Edition (PS5)','VIDEOJUEGOS','Catálogo de demostración Ñackson Store: Dead Cells: Return to Castlevania Edition (PS5). Verificar existencia física/fecha de lanzamiento','Físico','Videojuego','PS5',39990,10),
('Death Stranding Director’s Cut (PS5)','VIDEOJUEGOS','Catálogo de demostración Ñackson Store: Death Stranding Director’s Cut (PS5). Verificar existencia física/fecha de lanzamiento','Físico','Videojuego','PS5',39990,10),
('Death Stranding 2: On The Beach (PS5)','VIDEOJUEGOS','Catálogo de demostración Ñackson Store: Death Stranding 2: On The Beach (PS5). Verificar existencia física/fecha de lanzamiento','Físico','Videojuego','PS5',39990,10),
('Demons Night Fever (PS5)','VIDEOJUEGOS','Catálogo de demostración Ñackson Store: Demons Night Fever (PS5). Verificar existencia física/fecha de lanzamiento','Físico','Videojuego','PS5',39990,10),
('Demon’s Souls (PS5)','VIDEOJUEGOS','Catálogo de demostración Ñackson Store: Demon’s Souls (PS5). Verificar existencia física/fecha de lanzamiento','Físico','Videojuego','PS5',39990,10),
('Diablo IV (PS5)','VIDEOJUEGOS','Catálogo de demostración Ñackson Store: Diablo IV (PS5). Verificar existencia física/fecha de lanzamiento','Físico','Videojuego','PS5',39990,10),
('Digimon Story Time Stranger (Europeo, PS5)','VIDEOJUEGOS','Catálogo de demostración Ñackson Store: Digimon Story Time Stranger (Europeo, PS5). Verificar existencia física/fecha de lanzamiento','Físico','Videojuego','PS5',39990,10),
('Dragon Ball Xenoverse 2 (PS5)','VIDEOJUEGOS','Catálogo de demostración Ñackson Store: Dragon Ball Xenoverse 2 (PS5). Verificar existencia física/fecha de lanzamiento','Físico','Videojuego','PS5',39990,10),
('Final Fantasy Tactics: The Ivalice Chronicles (PS5)','VIDEOJUEGOS','Catálogo de demostración Ñackson Store: Final Fantasy Tactics: The Ivalice Chronicles (PS5). Verificar existencia física/fecha de lanzamiento','Físico','Videojuego','PS5',39990,10),
('Audífonos cerrados para estudio','ACCESORIOS','Catálogo de demostración Ñackson Store: Audífonos cerrados para estudio. Verificar modelo y precio demo provisional','Físico','Audio / Tecnología',NULL,29990,8),
('Monitor de estudio activo KRK V8 Series Five 8 pulgadas 455 W','ACCESORIOS','Catálogo de demostración Ñackson Store: Monitor de estudio activo KRK V8 Series Five 8 pulgadas 455 W. Verificar modelo y precio demo provisional','Físico','Audio / Tecnología',NULL,29990,8),
('Amplificador de monitoreo personal Behringer P1','ACCESORIOS','Catálogo de demostración Ñackson Store: Amplificador de monitoreo personal Behringer P1. Verificar modelo y precio demo provisional','Físico','Audio / Tecnología',NULL,29990,8),
('Amplificador de auriculares Behringer AMP800 V2 de 4 canales','ACCESORIOS','Catálogo de demostración Ñackson Store: Amplificador de auriculares Behringer AMP800 V2 de 4 canales. Verificar modelo y precio demo provisional','Físico','Audio / Tecnología',NULL,29990,8),
('Llavero / Keychain - One Piece','MERCHANDISING','Catálogo de demostración Ñackson Store: Llavero / Keychain - One Piece. Verificar edición/tamaño','Físico','Coleccionable',NULL,12990,10),
('Estuche Nintendo Switch Poké Ball','MERCHANDISING','Catálogo de demostración Ñackson Store: Estuche Nintendo Switch Poké Ball. Verificar edición/tamaño','Físico','Coleccionable',NULL,12990,10),
('Charizard EX Special Collection - Español','MERCHANDISING','Catálogo de demostración Ñackson Store: Charizard EX Special Collection - Español. Verificar edición/tamaño','Físico','Coleccionable',NULL,12990,10),
('Mega Evolution (Base Set) Booster Bundle - Inglés','MERCHANDISING','Catálogo de demostración Ñackson Store: Mega Evolution (Base Set) Booster Bundle - Inglés. Verificar edición/tamaño','Físico','Coleccionable',NULL,12990,10),
('Mega Evolution Chaos Rising Elite Trainer Box - Inglés','MERCHANDISING','Catálogo de demostración Ñackson Store: Mega Evolution Chaos Rising Elite Trainer Box - Inglés. Verificar edición/tamaño','Físico','Coleccionable',NULL,12990,10),
('One Piece - Zoro Viñetas - Polera','MERCHANDISING','Catálogo de demostración Ñackson Store: One Piece - Zoro Viñetas - Polera. Verificar edición/tamaño','Físico','Coleccionable',NULL,12990,10),
('Kiki Delivery Service - Jiji - Polera','MERCHANDISING','Catálogo de demostración Ñackson Store: Kiki Delivery Service - Jiji - Polera. Verificar edición/tamaño','Físico','Coleccionable',NULL,12990,10),
('Slam Dunk - Collage - Polera','MERCHANDISING','Catálogo de demostración Ñackson Store: Slam Dunk - Collage - Polera. Verificar edición/tamaño','Físico','Coleccionable',NULL,12990,10),
('Destapador Halcón Milenario','MERCHANDISING','Catálogo de demostración Ñackson Store: Destapador Halcón Milenario. Verificar edición/tamaño','Físico','Coleccionable',NULL,12990,10),
('Botella Termo Hogwarts 530 ml - Harry Potter','MERCHANDISING','Catálogo de demostración Ñackson Store: Botella Termo Hogwarts 530 ml - Harry Potter. Verificar edición/tamaño','Físico','Coleccionable',NULL,12990,10),
('Taza Mr. Meeseeks - Rick y Morty','MERCHANDISING','Catálogo de demostración Ñackson Store: Taza Mr. Meeseeks - Rick y Morty. Verificar edición/tamaño','Físico','Coleccionable',NULL,12990,10),
('Orb: On the Movements of the Earth - Bag Charm','MERCHANDISING','Catálogo de demostración Ñackson Store: Orb: On the Movements of the Earth - Bag Charm. Verificar edición/tamaño','Físico','Coleccionable',NULL,12990,10);

START TRANSACTION;

INSERT INTO producto (categoria,descripcion,estado,formato,genero,imagen_url,nombre,plataforma,precio_base)
SELECT s.categoria,s.descripcion,'ACTIVO',s.formato,s.genero,NULL,s.nombre,s.plataforma,s.precio_base
FROM tmp_nackson_lote s WHERE NOT EXISTS (SELECT 1 FROM producto p WHERE p.nombre=s.nombre AND p.categoria=s.categoria);

INSERT INTO inventario(product_id,stock_actual,umbral_critico)
SELECT p.id,s.stock_actual,5 FROM tmp_nackson_lote s
JOIN producto p ON p.nombre=s.nombre AND p.categoria=s.categoria
WHERE NOT EXISTS (SELECT 1 FROM inventario i WHERE i.product_id=p.id);

COMMIT;

SELECT COUNT(*) AS productos_lote FROM tmp_nackson_lote;

DROP TEMPORARY TABLE tmp_nackson_lote;