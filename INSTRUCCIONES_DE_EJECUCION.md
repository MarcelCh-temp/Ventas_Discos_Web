# Ñackson Store — Instrucciones de ejecución

## 1. Obtener el proyecto

Rama candidata de integración:
fix/correcciones-finales-nackson

Una vez publicada, ejecutar:

git fetch origin
git switch --track origin/fix/correcciones-finales-nackson

Si la rama ya existe localmente, utilizar git switch con su nombre.

Antes de cambiar de rama, respaldar las modificaciones locales.
No utilizar git reset --hard ni sobrescribir archivos del equipo.

## 2. Requisitos

- Java 25.
- Maven Wrapper incluido en los microservicios.
- Node.js y npm compatibles con el frontend.
- MariaDB/MySQL.
- XAMPP para el entorno local utilizado.
- Git.

MariaDB 10.4.32 permitió iniciar los servicios en el entorno
local, pero está por debajo del mínimo indicado por Hibernate.
Una actualización del motor requiere coordinación del equipo.

## 3. Variables de entorno

Los archivos .env reales no se incluyen en la nueva versión.

Cada integrante debe crear su propio .env a partir de
.env.example cuando no exista uno previamente.

Configurar las credenciales de su motor de base de datos.
Mantener consistentes las claves JWT, emisores y claves
internas utilizadas por los servicios.

No copiar credenciales reales al repositorio.
No sobrescribir configuraciones locales existentes.

Las credenciales históricas que hayan quedado expuestas
deben revisarse y rotarse antes de utilizar el sistema.

## 4. Bases de datos

| Microservicio | Base de datos | Script base |
|---|---|---|
| Accounts | db_accounts | Bases-De-Datos/db_accounts.sql |
| Catalog | db_catalog | Bases-De-Datos/db_catalog.sql |
| Ordenes | bd_ordenes | Bases-De-Datos/bd_ordenes.sql |
| PostSale | bd_ms_postsale | 3° Back-End/ms-postsale/db/db_ms_postsale.sql |
| Notifications | bd_ms_notifications | 3° Back-End/ms-notifications/db/db_ms_notifications.sql |
| Soporte | bd_soporte | Bases-De-Datos/bd_soporte.sql |

Los scripts base se deben revisar antes de utilizarlos
en instalaciones nuevas.

No importar dumps completos sobre bases existentes.

## 5. Migraciones para instalaciones existentes

Respaldar las bases de datos y detener los microservicios
antes de ejecutar modificaciones estructurales.

Desde phpMyAdmin, revisar y ejecutar individualmente:

1. MIGRACION_DETALLE_PEDIDO.sql
2. MIGRACION_ESTRUCTURA_POSTSALE.sql
3. DIAGNOSTICO_POSTSALE.sql
4. PREPARAR_RELACIONES_POSTSALE.sql
5. REPARAR_RELACIONES_SOPORTE.sql
6. VERIFICAR_SEIS_BD.sql

Todos están en Bases-De-Datos.

Postventa:

- Verificar los tipos de columnas e índices.
- No inventar información de solicitudes históricas.
- Si existen registros sin migrar, duplicados o referencias
  incompatibles, detener el procedimiento.
- Confirmar que datos_compatibles sea igual a 1 antes de
  modificar relaciones.
- Las relaciones nuevas deben existir antes de retirar
  las restricciones históricas.
- PREPARAR_RELACIONES_POSTSALE.sql solamente propone
  el retiro de las claves foráneas antiguas.
- Revisar y aprobar dichas propuestas con el equipo.
- No eliminar tablas históricas ni sus registros.

La nueva migración estructural todavía requiere una
ejecución de prueba controlada.

## 6. Puertos

| Servicio | Puerto |
|---|---|
| Accounts | 8081 |
| Catalog | 8082 |
| Ordenes | 8084 |
| PostSale | 8085 |
| Notifications | 8086 |
| Soporte | 8087 |
| API Gateway | 8080 o 8090 |
| Frontend Vite | 5173 |

## 7. Iniciar los microservicios

Abrir una terminal independiente para cada microservicio
desde su carpeta correspondiente en 3° Back-End.

Con las bases configuradas y migradas:

$env:DB_DDL_AUTO="validate"
.\mvnw.cmd spring-boot:run

Verificar que cada servicio inicie correctamente.

No cambiar el dialecto ni permitir modificaciones
automáticas del esquema sin revisar la configuración.

## 8. Iniciar API Gateway

Desde 3° Back-End/api-gateway:

.\mvnw.cmd spring-boot:run

Si el puerto 8080 está ocupado por Oracle u otro servicio:

$env:SERVER_PORT="8090"
.\mvnw.cmd spring-boot:run

## 9. Iniciar el frontend

Desde 2° Front-End:

npm.cmd ci

Configurar el puerto real del Gateway. Ejemplo:

$env:VITE_API_BASE_URL="http://localhost:8090"

npm.cmd run build
npm.cmd run dev

Acceder a:

http://localhost:5173

## 10. Pruebas

Frontend:

npm.cmd run test

Resultado local registrado el 26-11-2026:
98 pruebas aprobadas de 98, en 8 archivos.

Backend:

.\mvnw.cmd clean test

Ejecutar en cada microservicio y Gateway, utilizando
bases de prueba cuando corresponda.

Revisar PRUEBAS_PENDIENTES.md para las verificaciones
funcionales que todavía deben completarse.

Esta rama es una candidata de integración y pruebas.
No representa una certificación final del sistema.

## Precauciones para la integracion

- Respaldar los .env antes de cambiar de rama.
- Utilizar secretos JWT propios, distintos para usuarios y servicios.
- Accounts y Ordenes no incluyen registros personales ni transaccionales.
- Catalog conserva productos e inventario, sin movimientos historicos.
- MIGRACION_ESTRUCTURA_POSTSALE.sql utiliza sintaxis de MariaDB.
- No ejecutar esta migracion en MySQL sin adaptarla y validarla.
- Respaldar las bases existentes antes de ejecutar migraciones.
- Rama candidata de integracion con 951 pruebas automatizadas aprobadas.
- Pruebas funcionales integrales y migraciones pendientes de validacion.
