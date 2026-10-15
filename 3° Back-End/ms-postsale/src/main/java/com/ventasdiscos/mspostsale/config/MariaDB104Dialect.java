
package com.ventasdiscos.mspostsale.config;

import org.hibernate.dialect.MariaDBDialect;

public class MariaDB104Dialect extends MariaDBDialect {

    @Override
    public boolean supportsIfExistsAfterAlterTable() {
        return false;
    }
}
