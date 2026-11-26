-- phpMyAdmin SQL Dump
-- version 5.2.3
-- https://www.phpmyadmin.net/
--
-- Servidor: 127.0.0.1
-- Tiempo de generación: 21-11-2026 a las 20:42:06
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
-- Base de datos: `bd_soporte`
--

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `historial_estado_soporte`
--

CREATE TABLE `historial_estado_soporte` (
  `id` bigint(20) NOT NULL,
  `estado_anterior` enum('ABIERTA','CERRADA','EN_PROCESO') DEFAULT NULL,
  `estado_nuevo` enum('ABIERTA','CERRADA','EN_PROCESO') NOT NULL,
  `fecha_hora` datetime(6) NOT NULL,
  `usuario_responsable_id` bigint(20) DEFAULT NULL,
  `ticket_id` bigint(20) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `mensajes_soporte`
--

CREATE TABLE `mensajes_soporte` (
  `id` bigint(20) NOT NULL,
  `autor` enum('ADMINISTRADOR','CLIENTE') NOT NULL,
  `autor_id` bigint(20) DEFAULT NULL,
  `contenido` text NOT NULL,
  `fecha_envio` datetime(6) NOT NULL,
  `ticket_id` bigint(20) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `tickets`
--

CREATE TABLE `tickets` (
  `id` bigint(20) NOT NULL,
  `asunto` varchar(255) NOT NULL,
  `categoria` varchar(255) NOT NULL,
  `descripcion` text NOT NULL,
  `estado` enum('ABIERTA','CERRADA','EN_PROCESO') NOT NULL,
  `fecha_cierre` datetime(6) DEFAULT NULL,
  `fecha_creacion` datetime(6) NOT NULL,
  `id_usuario` bigint(20) NOT NULL,
  `numero_ticket` varchar(20) DEFAULT NULL,
  `order_id` bigint(20) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Índices para tablas volcadas
--

--
-- Indices de la tabla `historial_estado_soporte`
--
ALTER TABLE `historial_estado_soporte`
  ADD PRIMARY KEY (`id`),
  ADD KEY `FKckubyt92kd3xrxlq2gmn6fgn0` (`ticket_id`);

--
-- Indices de la tabla `mensajes_soporte`
--
ALTER TABLE `mensajes_soporte`
  ADD PRIMARY KEY (`id`),
  ADD KEY `FKjl7gi1wvjtmk5dirp91smls7d` (`ticket_id`);

--
-- Indices de la tabla `tickets`
--
ALTER TABLE `tickets`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `UKk0msfrh058q2goawy9u1sig33` (`numero_ticket`);

--
-- AUTO_INCREMENT de las tablas volcadas
--

--
-- AUTO_INCREMENT de la tabla `historial_estado_soporte`
--
ALTER TABLE `historial_estado_soporte`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT de la tabla `mensajes_soporte`
--
ALTER TABLE `mensajes_soporte`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT de la tabla `tickets`
--
ALTER TABLE `tickets`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT;

--
-- Restricciones para tablas volcadas
--

--
-- Filtros para la tabla `historial_estado_soporte`
--
ALTER TABLE `historial_estado_soporte`
  ADD CONSTRAINT `FKckubyt92kd3xrxlq2gmn6fgn0` FOREIGN KEY (`ticket_id`) REFERENCES `tickets` (`id`);

--
-- Filtros para la tabla `mensajes_soporte`
--
ALTER TABLE `mensajes_soporte`
  ADD CONSTRAINT `FKjl7gi1wvjtmk5dirp91smls7d` FOREIGN KEY (`ticket_id`) REFERENCES `tickets` (`id`);
COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
