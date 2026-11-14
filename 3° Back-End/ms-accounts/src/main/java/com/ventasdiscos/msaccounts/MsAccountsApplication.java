package com.ventasdiscos.msaccounts;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
// Componente del sistema: sus responsabilidades se explican en los métodos y campos. Clase MsAccountsApplication.
public class MsAccountsApplication {
    // Operación auxiliar que forma parte de la responsabilidad de esta clase. Método main (main).
    public static void main(String[] args) {
        SpringApplication.run(MsAccountsApplication.class, args);
    }
}
