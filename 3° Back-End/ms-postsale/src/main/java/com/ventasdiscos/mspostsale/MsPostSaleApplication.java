package com.ventasdiscos.mspostsale;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
// Componente del sistema: sus responsabilidades se explican en los métodos y campos. Clase MsPostSaleApplication.
public class MsPostSaleApplication {

    // Operación auxiliar que forma parte de la responsabilidad de esta clase. Método main (main).
    public static void main(String[] args) {
        SpringApplication.run(MsPostSaleApplication.class,args);
    }
}
