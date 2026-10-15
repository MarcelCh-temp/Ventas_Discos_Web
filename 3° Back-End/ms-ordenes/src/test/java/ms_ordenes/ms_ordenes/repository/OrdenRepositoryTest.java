package ms_ordenes.ms_ordenes.repository;

import ms_ordenes.ms_ordenes.model.EstadoOrden;
import ms_ordenes.ms_ordenes.model.Orden;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrdenRepositoryTest {

    @Mock
    private OrdenRepository ordenRepository;

    @Test
    void testFindByIdUsuario() {
        Orden orden = new Orden(100L, new BigDecimal("5000.00"));
        orden.setId(1L);
        orden.setEstado(EstadoOrden.PENDIENTE);

        when(ordenRepository.findByIdUsuario(100L)).thenReturn(List.of(orden));

        List<Orden> resultado = ordenRepository.findByIdUsuario(100L);

        assertEquals(1, resultado.size());
        assertEquals(100L, resultado.get(0).getIdUsuario());
    }
}