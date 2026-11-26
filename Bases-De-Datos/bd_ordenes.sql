-- phpMyAdmin SQL Dump
-- version 5.2.3
-- https://www.phpmyadmin.net/
--
-- Servidor: 127.0.0.1
-- Tiempo de generación: 21-11-2026 a las 20:41:44
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
-- Base de datos: `bd_ordenes`
--

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `carrito`
--

CREATE TABLE `carrito` (
  `id` bigint(20) NOT NULL,
  `fecha_actualizacion` datetime(6) NOT NULL,
  `total` decimal(12,2) NOT NULL,
  `user_id` bigint(20) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Volcado de datos para la tabla `carrito`
--

-- Datos transaccionales retirados del volcado.
-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `datos_entrega`
--

CREATE TABLE `datos_entrega` (
  `id` bigint(20) NOT NULL,
  `comuna` varchar(80) DEFAULT NULL,
  `direccion` varchar(150) DEFAULT NULL,
  `metodo_entrega` enum('DESPACHO_DOMICILIO','RETIRO_TIENDA') NOT NULL,
  `nombre_receptor` varchar(100) NOT NULL,
  `referencia` varchar(200) DEFAULT NULL,
  `telefono` varchar(20) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Volcado de datos para la tabla `datos_entrega`
--

-- Datos transaccionales retirados del volcado.
-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `despacho`
--

CREATE TABLE `despacho` (
  `id` bigint(20) NOT NULL,
  `fecha_despacho` datetime(6) NOT NULL,
  `numero_seguimiento` varchar(100) DEFAULT NULL,
  `transportista` varchar(100) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `historial_pedido`
--

CREATE TABLE `historial_pedido` (
  `id` bigint(20) NOT NULL,
  `descripcion` varchar(500) DEFAULT NULL,
  `estado_anterior` enum('CANCELADO','CONFIRMADO','DESPACHADO','ENTREGADO','PENDIENTE','PREPARANDO') DEFAULT NULL,
  `estado_nuevo` enum('CANCELADO','CONFIRMADO','DESPACHADO','ENTREGADO','PENDIENTE','PREPARANDO') NOT NULL,
  `fecha_hora` datetime(6) NOT NULL,
  `motivo` varchar(255) DEFAULT NULL,
  `usuario_responsable_id` bigint(20) DEFAULT NULL,
  `orden_id` bigint(20) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Volcado de datos para la tabla `historial_pedido`
--

-- Datos transaccionales retirados del volcado.
-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `item_carrito`
--

CREATE TABLE `item_carrito` (
  `id` bigint(20) NOT NULL,
  `cantidad` int(11) NOT NULL,
  `precio_unitario` decimal(12,2) NOT NULL,
  `product_id` bigint(20) NOT NULL,
  `carrito_id` bigint(20) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Volcado de datos para la tabla `item_carrito`
--

-- Datos transaccionales retirados del volcado.
-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `item_pedido`
--

CREATE TABLE `item_pedido` (
  `id` bigint(20) NOT NULL,
  `cantidad` int(11) NOT NULL,
  `nombre_producto_snapshot` varchar(150) NOT NULL,
  `precio_historico` decimal(12,2) NOT NULL,
  `product_id` bigint(20) NOT NULL,
  `subtotal_historico` decimal(12,2) NOT NULL,
  `orden_id` bigint(20) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Volcado de datos para la tabla `item_pedido`
--

-- Datos transaccionales retirados del volcado.
-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `ordenes`
--

CREATE TABLE `ordenes` (
  `id` bigint(20) NOT NULL,
  `descuento_historico` decimal(12,2) DEFAULT NULL,
  `estado` enum('CANCELADO','CONFIRMADO','DESPACHADO','ENTREGADO','PENDIENTE','PREPARANDO') NOT NULL,
  `fecha_creacion` datetime(6) NOT NULL,
  `id_usuario` bigint(20) NOT NULL,
  `numero_pedido` varchar(20) DEFAULT NULL,
  `total` decimal(12,2) NOT NULL,
  `datos_entrega_id` bigint(20) DEFAULT NULL,
  `despacho_id` bigint(20) DEFAULT NULL,
  `costo_envio` decimal(12,2) DEFAULT NULL,
  `descuento_ruleta` decimal(12,2) DEFAULT NULL,
  `premio_ruleta` varchar(25) DEFAULT NULL,
  `metodo_pago` varchar(25) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Volcado de datos para la tabla `ordenes`
--

-- Datos transaccionales retirados del volcado.
-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `ruleta_usuario`
--

CREATE TABLE `ruleta_usuario` (
  `user_id` bigint(20) NOT NULL,
  `ultimo_giro` datetime DEFAULT NULL,
  `premio` varchar(25) NOT NULL DEFAULT 'NINGUNO',
  `estado` varchar(25) NOT NULL DEFAULT 'SIN_GIRO',
  `pedido_id` bigint(20) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Volcado de datos para la tabla `ruleta_usuario`
--

-- Datos transaccionales retirados del volcado.
--
-- Índices para tablas volcadas
--

--
-- Indices de la tabla `carrito`
--
ALTER TABLE `carrito`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `UKq4r65qfvmoo5focjwog3r0ncp` (`user_id`);

--
-- Indices de la tabla `datos_entrega`
--
ALTER TABLE `datos_entrega`
  ADD PRIMARY KEY (`id`);

--
-- Indices de la tabla `despacho`
--
ALTER TABLE `despacho`
  ADD PRIMARY KEY (`id`);

--
-- Indices de la tabla `historial_pedido`
--
ALTER TABLE `historial_pedido`
  ADD PRIMARY KEY (`id`),
  ADD KEY `FKipn4fnvh2k30cieo5jedqpggq` (`orden_id`);

--
-- Indices de la tabla `item_carrito`
--
ALTER TABLE `item_carrito`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `uk_item_carrito_producto` (`carrito_id`,`product_id`);

--
-- Indices de la tabla `item_pedido`
--
ALTER TABLE `item_pedido`
  ADD PRIMARY KEY (`id`),
  ADD KEY `FK3v1al5ga28o83n8y25922uy6` (`orden_id`);

--
-- Indices de la tabla `ordenes`
--
ALTER TABLE `ordenes`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `UKxvr42qsblttu4bd4w3c9daiy` (`numero_pedido`),
  ADD UNIQUE KEY `UKq43idi774tnyt0xqmp7i5b96p` (`datos_entrega_id`),
  ADD UNIQUE KEY `UK3njkcro1pw2qhbq1j0bk39nbn` (`despacho_id`);

--
-- Indices de la tabla `ruleta_usuario`
--
ALTER TABLE `ruleta_usuario`
  ADD PRIMARY KEY (`user_id`),
  ADD KEY `idx_ruleta_pedido` (`pedido_id`);

--
-- AUTO_INCREMENT de las tablas volcadas
--

--
-- AUTO_INCREMENT de la tabla `carrito`
--
ALTER TABLE `carrito`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=3;

--
-- AUTO_INCREMENT de la tabla `datos_entrega`
--
ALTER TABLE `datos_entrega`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=3;

--
-- AUTO_INCREMENT de la tabla `despacho`
--
ALTER TABLE `despacho`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT de la tabla `historial_pedido`
--
ALTER TABLE `historial_pedido`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=6;

--
-- AUTO_INCREMENT de la tabla `item_carrito`
--
ALTER TABLE `item_carrito`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=11;

--
-- AUTO_INCREMENT de la tabla `item_pedido`
--
ALTER TABLE `item_pedido`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=3;

--
-- AUTO_INCREMENT de la tabla `ordenes`
--
ALTER TABLE `ordenes`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=3;

--
-- Restricciones para tablas volcadas
--

--
-- Filtros para la tabla `historial_pedido`
--
ALTER TABLE `historial_pedido`
  ADD CONSTRAINT `FKipn4fnvh2k30cieo5jedqpggq` FOREIGN KEY (`orden_id`) REFERENCES `ordenes` (`id`);

--
-- Filtros para la tabla `item_carrito`
--
ALTER TABLE `item_carrito`
  ADD CONSTRAINT `FK7jlhnuuh4m7q1cn77xhtx9kdq` FOREIGN KEY (`carrito_id`) REFERENCES `carrito` (`id`);

--
-- Filtros para la tabla `item_pedido`
--
ALTER TABLE `item_pedido`
  ADD CONSTRAINT `FK3v1al5ga28o83n8y25922uy6` FOREIGN KEY (`orden_id`) REFERENCES `ordenes` (`id`);

--
-- Filtros para la tabla `ordenes`
--
ALTER TABLE `ordenes`
  ADD CONSTRAINT `FK4qj5ssp6xox9850ojukcvk1tl` FOREIGN KEY (`despacho_id`) REFERENCES `despacho` (`id`),
  ADD CONSTRAINT `FKd58e0ox2k6hywpj92elb41dfq` FOREIGN KEY (`datos_entrega_id`) REFERENCES `datos_entrega` (`id`);
COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
