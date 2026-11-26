-- Requiere respaldo y MS-PostSale detenido. Ejecutar DIAGNOSTICO_POSTSALE primero.
-- No elimina filas ni tablas. Si hay datos históricos sin migrar, detenerse.
USE bd_ms_postsale;
SET @correctos = ((SELECT COUNT(*) FROM solicitudes_postventa WHERE tipo IS NULL OR estado IS NULL OR tipo='' OR estado='')=0
AND (SELECT COUNT(*) FROM items_devolucion d LEFT JOIN solicitudes_postventa s ON s.id=d.solicitud_id WHERE s.id IS NULL OR s.tipo IS NULL OR s.tipo <> 'DEVOLUCION')=0
AND (SELECT COUNT(*) FROM recepciones_devolucion d LEFT JOIN solicitudes_postventa s ON s.id=d.solicitud_id WHERE s.id IS NULL OR s.tipo IS NULL OR s.tipo <> 'DEVOLUCION')=0);
SELECT @correctos AS datos_compatibles;
SET @ddl=IF(@correctos=1 AND (SELECT COUNT(*) FROM information_schema.KEY_COLUMN_USAGE WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='items_devolucion' AND COLUMN_NAME='solicitud_id' AND REFERENCED_TABLE_NAME='solicitudes_postventa' AND REFERENCED_COLUMN_NAME='id')=0,
'ALTER TABLE items_devolucion ADD CONSTRAINT fk_item_devolucion_postventa FOREIGN KEY (solicitud_id) REFERENCES solicitudes_postventa(id)','SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @ddl=IF(@correctos=1 AND (SELECT COUNT(*) FROM information_schema.KEY_COLUMN_USAGE WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='recepciones_devolucion' AND COLUMN_NAME='solicitud_id' AND REFERENCED_TABLE_NAME='solicitudes_postventa' AND REFERENCED_COLUMN_NAME='id')=0,
'ALTER TABLE recepciones_devolucion ADD CONSTRAINT fk_recepcion_devolucion_postventa FOREIGN KEY (solicitud_id) REFERENCES solicitudes_postventa(id)','SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
-- Solo genera el texto de las restricciones históricas que deben retirarse, NO ejecuta el DROP.
-- Revisar el resultado con el equipo; las restricciones nuevas deben existir antes de retirar las antiguas.
SELECT CONCAT('ALTER TABLE `', k.TABLE_NAME, '` DROP FOREIGN KEY `', k.CONSTRAINT_NAME, '`;') AS propuesta_retirar_fk_antigua
FROM information_schema.KEY_COLUMN_USAGE k WHERE @correctos=1 AND k.TABLE_SCHEMA=DATABASE() AND k.TABLE_NAME IN ('items_devolucion','recepciones_devolucion') AND k.COLUMN_NAME='solicitud_id' AND k.REFERENCED_TABLE_NAME='solicitudes_devolucion'
AND EXISTS(SELECT 1 FROM information_schema.KEY_COLUMN_USAGE n WHERE n.TABLE_SCHEMA=k.TABLE_SCHEMA AND n.TABLE_NAME=k.TABLE_NAME AND n.COLUMN_NAME='solicitud_id' AND n.REFERENCED_TABLE_NAME='solicitudes_postventa' AND n.REFERENCED_COLUMN_NAME='id');
