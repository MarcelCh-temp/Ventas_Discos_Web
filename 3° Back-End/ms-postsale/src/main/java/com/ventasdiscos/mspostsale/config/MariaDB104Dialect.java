
package com.ventasdiscos.mspostsale.config;

import org.hibernate.dialect.MariaDBDialect;

// Configuración: prepara componentes y parámetros necesarios para Spring. Clase MariaDB104Dialect.
public class MariaDB104Dialect extends MariaDBDialect {

    @Override
    // Configuración reutilizable del framework o de la aplicación. Método supportsIfExistsAfterAlterTable (supports if exists after alter table).
    public boolean supportsIfExistsAfterAlterTable() {
        return false;
    }
}
