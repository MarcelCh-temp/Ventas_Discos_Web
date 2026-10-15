package ms_ordenes.ms_ordenes.service;

import ms_ordenes.ms_ordenes.model.EstadoOrden;
import ms_ordenes.ms_ordenes.model.Orden;
import ms_ordenes.ms_ordenes.repository.OrdenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrdenServiceTest {

    @Mock
    private OrdenRepository ordenRepository;

    @InjectMocks
    private OrdenService ordenService;

    private Orden ordenPrueba;

    @BeforeEach
    void setUp() {
        ordenPrueba = new Orden(1L, new BigDecimal("29990.00"));
        ordenPrueba.setId(1L);
        ordenPrueba.setEstado(EstadoOrden.PENDIENTE);
    }

    @Test
    void crearOrden_GuardaYRetornaOrden() {
        when(ordenRepository.save(any(Orden.class))).thenReturn(ordenPrueba);

        Orden resultado = ordenService.crearOrden(ordenPrueba);

        assertNotNull(resultado);
        assertEquals(1L, resultado.getId());
        assertEquals(new BigDecimal("29990.00"), resultado.getTotal());
        assertEquals(EstadoOrden.PENDIENTE, resultado.getEstado());
    }

    @Test
    void obtenerTodas_DevuelveListaDeOrdenes() {
        when(ordenRepository.findAll()).thenReturn(List.of(ordenPrueba));

        List<Orden> resultado = ordenService.obtenerTodas();

        assertFalse(resultado.isEmpty());
        assertEquals(1, resultado.size());
    }
}