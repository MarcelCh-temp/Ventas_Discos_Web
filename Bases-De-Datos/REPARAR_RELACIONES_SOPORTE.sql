-- Ejecutar solo con respaldo y MS-Soporte detenido. No elimina filas ni restricciones.
USE bd_soporte;
SELECT 'historial_estado_soporte' tabla, COUNT(*) huerfanos FROM historial_estado_soporte h LEFT JOIN tickets t ON h.ticket_id=t.id WHERE h.ticket_id IS NOT NULL AND t.id IS NULL
UNION ALL SELECT 'mensajes_soporte', COUNT(*) FROM mensajes_soporte m LEFT JOIN tickets t ON m.ticket_id=t.id WHERE m.ticket_id IS NOT NULL AND t.id IS NULL;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.KEY_COLUMN_USAGE WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='historial_estado_soporte' AND COLUMN_NAME='ticket_id' AND REFERENCED_TABLE_NAME='tickets' AND REFERENCED_COLUMN_NAME='id')=0
AND (SELECT COUNT(*) FROM historial_estado_soporte h LEFT JOIN tickets t ON h.ticket_id=t.id WHERE h.ticket_id IS NOT NULL AND t.id IS NULL)=0,
'ALTER TABLE historial_estado_soporte ADD CONSTRAINT FKckubyt92kd3xrxlq2gmn6fgn0 FOREIGN KEY (ticket_id) REFERENCES tickets(id)', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.KEY_COLUMN_USAGE WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='mensajes_soporte' AND COLUMN_NAME='ticket_id' AND REFERENCED_TABLE_NAME='tickets' AND REFERENCED_COLUMN_NAME='id')=0
AND (SELECT COUNT(*) FROM mensajes_soporte m LEFT JOIN tickets t ON m.ticket_id=t.id WHERE m.ticket_id IS NOT NULL AND t.id IS NULL)=0,
'ALTER TABLE mensajes_soporte ADD CONSTRAINT FKjl7gi1wvjtmk5dirp91smls7d FOREIGN KEY (ticket_id) REFERENCES tickets(id)', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SELECT TABLE_NAME,CONSTRAINT_NAME,COLUMN_NAME,REFERENCED_TABLE_NAME FROM information_schema.KEY_COLUMN_USAGE WHERE TABLE_SCHEMA=DATABASE() AND REFERENCED_TABLE_NAME IS NOT NULL;
