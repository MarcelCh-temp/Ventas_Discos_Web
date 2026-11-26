-- Auditoría no destructiva: motor, tablas y claves foráneas.
SELECT VERSION() AS version_servidor, @@version_comment AS motor;
SELECT TABLE_SCHEMA,TABLE_NAME,ENGINE FROM information_schema.TABLES WHERE TABLE_SCHEMA IN ('db_accounts','db_catalog','bd_ordenes','bd_ms_postsale','bd_ms_notifications','bd_soporte') ORDER BY TABLE_SCHEMA,TABLE_NAME;
SELECT TABLE_SCHEMA,TABLE_NAME,CONSTRAINT_NAME,COLUMN_NAME,REFERENCED_TABLE_NAME,REFERENCED_COLUMN_NAME FROM information_schema.KEY_COLUMN_USAGE WHERE TABLE_SCHEMA IN ('db_accounts','db_catalog','bd_ordenes','bd_ms_postsale','bd_ms_notifications','bd_soporte') AND REFERENCED_TABLE_NAME IS NOT NULL ORDER BY TABLE_SCHEMA,TABLE_NAME;
