package ms_ordenes.ms_ordenes.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import ms_ordenes.ms_ordenes.model.EstadoOrden;
import ms_ordenes.ms_ordenes.model.Orden;
import ms_ordenes.ms_ordenes.service.OrdenService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrdenController.class)
@AutoConfigureMockMvc(addFilters = false)
public class OrdenControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrdenService ordenService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void crearOrden_EndpointPost_Devuelve201() throws Exception {
        Orden orden = new Orden(1L, new BigDecimal("29990.00"));
        orden.setId(1L);

        when(ordenService.crearOrden(any(Orden.class))).thenReturn(orden);

        mockMvc.perform(post("/api/ordenes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(orden)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.total").value(29990.00));
    }

    @Test
    void listarOrdenes_EndpointGet_DevuelveLista() throws Exception {
        Orden orden = new Orden(1L, new BigDecimal("29990.00"));
        orden.setId(1L);

        when(ordenService.obtenerTodas()).thenReturn(List.of(orden));

        mockMvc.perform(get("/api/ordenes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
    }
}