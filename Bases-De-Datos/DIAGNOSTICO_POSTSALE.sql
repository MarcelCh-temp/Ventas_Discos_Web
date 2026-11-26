-- Solo lectura. Ejecutar tras respaldar y arrancar PostSale una vez.
USE bd_ms_postsale;
SELECT TABLE_NAME FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE() ORDER BY TABLE_NAME;
SELECT TABLE_NAME, COLUMN_NAME, COLUMN_TYPE FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME IN ('solicitudes_postventa','solicitudes_devolucion','solicitudes_cancelacion','solicitudes_reembolso') ORDER BY TABLE_NAME,ORDINAL_POSITION;
SELECT TABLE_NAME, CONSTRAINT_NAME, COLUMN_NAME, REFERENCED_TABLE_NAME, REFERENCED_COLUMN_NAME FROM information_schema.KEY_COLUMN_USAGE WHERE TABLE_SCHEMA=DATABASE() AND REFERENCED_TABLE_NAME IS NOT NULL ORDER BY TABLE_NAME;
-- Estas consultas requieren las tablas/columnas de la entidad actual.
SELECT COUNT(*) AS solicitudes_sin_tipo_o_estado FROM solicitudes_postventa WHERE tipo IS NULL OR estado IS NULL OR tipo='' OR estado='';
SELECT 'items_devolucion' tabla, COUNT(*) AS referencias_invalidas FROM items_devolucion d LEFT JOIN solicitudes_postventa s ON s.id=d.solicitud_id WHERE s.id IS NULL OR s.tipo IS NULL OR s.tipo <> 'DEVOLUCION'
UNION ALL SELECT 'recepciones_devolucion', COUNT(*) FROM recepciones_devolucion d LEFT JOIN solicitudes_postventa s ON s.id=d.solicitud_id WHERE s.id IS NULL OR s.tipo IS NULL OR s.tipo <> 'DEVOLUCION';
-- Si hay filas sin tipo/estado, no borrar tablas hijas ni modificar FKs: primero migrar sus datos con el equipo.
