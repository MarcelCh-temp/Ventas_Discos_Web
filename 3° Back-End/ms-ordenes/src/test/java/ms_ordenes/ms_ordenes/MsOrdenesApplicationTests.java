package ms_ordenes.ms_ordenes;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertNotNull;

// Componente del sistema: sus responsabilidades se explican en los métodos y campos. Clase MsOrdenesApplicationTests.
class MsOrdenesApplicationTests {

    @Test
    void testApplicationInstantiation() {
        MsOrdenesApplication app = new MsOrdenesApplication();
        assertNotNull(app);
    }
}