package ms_ordenes.ms_ordenes;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
// Componente del sistema: sus responsabilidades se explican en los métodos y campos. Clase MsOrdenesApplication.
public class MsOrdenesApplication {

	// Operación auxiliar que forma parte de la responsabilidad de esta clase. Método main (main).
	public static void main(String[] args) {
		SpringApplication.run(MsOrdenesApplication.class, args);
	}

}
