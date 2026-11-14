package com.ventasdiscos.msnotifications;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
// Componente del sistema: sus responsabilidades se explican en los métodos y campos. Clase MsNotificationsApplication.
public class MsNotificationsApplication {

	// Operación auxiliar que forma parte de la responsabilidad de esta clase. Método main (main).
	public static void main(String[] args) {
		SpringApplication.run(MsNotificationsApplication.class, args);
	}

}
